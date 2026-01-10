package dev.booky.betterview.common.antixray;
// Created by booky10 in BetterView (19:40 16.06.2025)

import dev.booky.betterview.common.config.BvLevelConfig.AntiXrayConfig;
import dev.booky.betterview.common.util.MathUtil;
import dev.booky.betterview.common.util.VarIntUtil;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * This is a simple anti-xray, which would break under normal circumstances but
 * works for our use-case as we are only responsible for writing chunks far away.
 * <p>
 * This means we can just skip all the complex logic responsible for handling neighboring
 * chunks and checks whether a block is exposed to air/liquid or not and just replace
 * all unwanted blocks with another type of block.
 * <p>
 * The replacement of blocks needs to be platform-specific as we don't want to wrap
 * each paletted block state container.
 */
@NullMarked
public final class AntiXrayProcessor {

    private static final long[] EMPTY_LONG_ARRAY = new long[0];

    private static final int STORAGE_BITS = 4;
    private static final int STORAGE_SIZE = 1 << STORAGE_BITS;
    private static final int STORAGE_SIZE_2D = 1 << STORAGE_BITS * 2;
    private static final int STORAGE_SIZE_3D = 1 << STORAGE_BITS * 3;

    private final ReplacementStrategy.Ctor strategy;
    private final ReplacementPresets presets;
    private final BitSet obfuscatedStates;
    private final int stateRegistrySize;

    public AntiXrayProcessor(
            ReplacementStrategy.Ctor strategy, ReplacementPresets presets,
            int[] obfuscatedStates, int stateRegistrySize
    ) {
        this.strategy = strategy;
        this.presets = presets;
        this.stateRegistrySize = stateRegistrySize;

        this.obfuscatedStates = BitSet.valueOf(EMPTY_LONG_ARRAY);
        this.obfuscatedStates.set(stateRegistrySize); // expand BitSet
        for (int i = 0, len = obfuscatedStates.length; i < len; i++) {
            this.obfuscatedStates.set(obfuscatedStates[i]);
        }
    }

    public static @Nullable AntiXrayProcessor createProcessor(
            AntiXrayConfig config,
            ReplacementPresets levelPresets,
            Function<Key, Stream<Integer>> stateListFn,
            int stateRegistrySize
    ) {
        if (!config.isEnabled()) {
            return null;
        }
        // convert list of hidden blocks to blockstate ids
        int[] states = config.getHiddenBlocks().stream()
                .flatMap(stateListFn)
                .mapToInt(i -> i)
                .distinct()
                .toArray();
        return new AntiXrayProcessor(
                config.getEngineMode().getStrategy(),
                // use base block of level for hide-mode, otherwise use obfuscated states
                config.getEngineMode() == AntiXrayConfig.EngineMode.HIDE ? levelPresets :
                        // use replaced states for obfuscation mode
                        ReplacementPresets.createStatic(states),
                states, stateRegistrySize
        );
    }

    /**
     * @return the index in the storage array for the specific block
     */
    private static int storageIndex(int blockX, int blockY, int blockZ) {
        return (((blockY << STORAGE_BITS) | blockZ) << STORAGE_BITS) | blockX;
    }

    /**
     * This method takes in already combined X and Z coordinates for performance.
     *
     * @return the index in the storage array for the specific block
     */
    private static int storageIndex(int blockXZ, int blockY) {
        return (blockY << (STORAGE_BITS * 2)) | blockXZ;
    }

    /**
     * This needs to be called immediately after the paletted blockstate container has been written,
     * with the reader index positioned in front of the written data.
     *
     * @param sectionY      the signed Y position of the section
     * @param storageLength whether to write length-prefix for the storage array (false for 1.21.5+)
     */
    public synchronized void process(ByteBuf buf, int sectionY, boolean storageLength) {
        int[] presets = this.presets.getPresets(sectionY);
        int presetCount = presets.length;
        if (presetCount < 1) {
            return;
        }

        int readerIndex = buf.readerIndex();
        int paletteBits = buf.readByte();
        int paletteStorageBits = paletteBits;
        int newPaletteBits = paletteBits;

        // new palette contents (if they need to be replaced)
        int[] newPalette = null;
        // allows to check whether a palette entry needs to be obfuscated
        BitSet obfuscatedPalette = null;
        // the presets array, but with palette indices of the preset blockstates as a value
        int[] presetPalette = null;

        switch (paletteBits) {
            // single value palette
            case 0: {
                int value = VarIntUtil.readVarInt(buf);
                if (!this.obfuscatedStates.get(value)) {
                    return; // most common case for sections full of air
                } else if (presetCount == 1 && presets[0] == value) {
                    return; // if the only block is also our preset block, there is nothing to do
                }
                // only entry 0 needs to be obfuscated
                obfuscatedPalette = BitSet.valueOf(EMPTY_LONG_ARRAY);
                obfuscatedPalette.set(0);
                presetPalette = new int[presetCount];
                // search for the palette value in our presets to determine
                // whether the value may also be a preset block
                int presetIndex = Arrays.binarySearch(presets, value);
                if (presetIndex < 0) {
                    // construct new palette, value not in presets
                    newPalette = new int[1 + presetCount];
                    System.arraycopy(presets, 0, newPalette, 1, presetCount);
                    // specify indices of preset blockstate in the palette
                    for (int i = 0; i < presetCount; i++) {
                        presetPalette[i] = i + 1;
                    }
                } else {
                    // construct new palette, value is in presets
                    newPalette = new int[presetCount];
                    // copy the preset blockstate ids around the value into the new palette array
                    System.arraycopy(presets, 0,
                            newPalette, 1, presetIndex);
                    System.arraycopy(presets, presetIndex + 1,
                            newPalette, 1 + presetIndex, presetCount - (presetIndex + 1));
                    for (int i = 0; i < presetIndex; i++) {
                        presetPalette[i] = i + 1;
                    }
                    for (int i = presetIndex + 1; i < presetCount; i++) {
                        presetPalette[i] = i;
                    }
                    presetPalette[presetIndex] = 0;
                }
                // don't move single value away from position 0
                newPalette[0] = value;
                // when resizing a single-value palette, we will most likely be using a linear/hashmap
                // palette; if so, specify that we need at least 4 bits, as the linear palette
                // always expects to read 4 bits per entry
                newPaletteBits = Math.max(4, MathUtil.ceilLog2(newPalette.length));
                break;
            }
            // linear palette
            case 1, 2, 3, 4:
                paletteStorageBits = 4;
                newPaletteBits = 4;
                // fall through to hashmap palette, same network structure
            case 5, 6, 7, 8: {
                int paletteSize = VarIntUtil.readVarInt(buf);
                int[] palette = new int[paletteSize];
                // the count of the preset states not included in the existing palette,
                // to figure out whether we need to resize the palette or not
                int extraPaletteSize = presetCount;
                for (int i = 0; i < paletteSize; i++) {
                    int value = VarIntUtil.readVarInt(buf);
                    palette[i] = value;
                    // check if this blockstate needs to be obfuscated
                    if (this.obfuscatedStates.get(value)
                            // don't obfuscate if this value is the only replacement state anyway
                            && (presetCount != 1 || presets[0] != value)) {
                        // lazy-load obfuscated palette bitset
                        if (obfuscatedPalette == null) {
                            obfuscatedPalette = BitSet.valueOf(EMPTY_LONG_ARRAY);
                            obfuscatedPalette.set(paletteSize); // expand
                        }
                        // mark current palette entry for obfuscation
                        obfuscatedPalette.set(i);
                    }
                    // check if this blockstate is present in our presets array
                    int presetIndex = Arrays.binarySearch(presets, value);
                    if (presetIndex >= 0) {
                        extraPaletteSize--; // one less preset state to add
                        // save index of palette entry
                        if (presetPalette == null) {
                            presetPalette = new int[presetCount];
                            if (paletteSize != 1) { // don't fill array if there is only one preset anyway
                                Arrays.fill(presetPalette, -1);
                            }
                        }
                        presetPalette[presetIndex] = i;
                    }
                }
                if (obfuscatedPalette == null) {
                    return; // nothing to obfuscate, cancel processing
                }
                // check if we need to modify the palette; if extraPaletteSize
                // is zero, all presets are already present in the existing palette, and
                // we don't need to do anything
                if (extraPaletteSize > 0) {
                    newPalette = new int[paletteSize + extraPaletteSize];
                    // copy in original palette, don't modify original indices
                    System.arraycopy(palette, 0, newPalette, 0, paletteSize);
                    // check whether any of the preset blockstates are present in the existing palette
                    if (presetPalette != null) {
                        // some presets present in existing palette,
                        // copy over remaining presets
                        for (int i = 0, j = paletteSize; i < presetCount; i++) {
                            if (presetPalette[i] == -1) {
                                newPalette[j] = presets[i]; // push into new palette
                                presetPalette[i] = j++; // save index in new palette
                            }
                        }
                    } else {
                        // no presets present at all, just append our preset blockstates into the new palette...
                        System.arraycopy(presets, 0, newPalette, paletteSize, presetCount);
                        // ... and calculate indices of preset blockstates
                        presetPalette = new int[presetCount];
                        for (int i = 0; i < presetCount; i++) {
                            presetPalette[i] = i + paletteSize;
                        }
                    }
                    // update bits-per-entry of new palette, but don't allow shrinking the bits-per-entry;
                    // the linear palette has a hard-coded bits-per-entry of 4
                    int predictedBits = MathUtil.ceilLog2(paletteSize + extraPaletteSize);
                    newPaletteBits = Math.max(predictedBits, newPaletteBits);
                }
                break;
            }
            // global palette
            default: {
                paletteStorageBits = MathUtil.ceilLog2(this.stateRegistrySize);
                newPaletteBits = paletteStorageBits;
                // the easy one :)
                obfuscatedPalette = this.obfuscatedStates;
                presetPalette = presets;
            }
        }

        // so, at this stage we have figured out we need to obfuscate some blocks
        // as we've read the palette; the next step is read the storage and
        // perform the obfuscation, while at the same time resizing the storage
        // array if we've modified the palette

        // read storage data from buffer
        int storageIndex = buf.readerIndex();
        long entryMask;
        int valuesPerWord;
        long[] storage;
        if (paletteStorageBits == 0) {
            entryMask = 0;
            valuesPerWord = 0; // dummy value
            if (storageLength) {
                // verify storage length in buffer
                int bufWordCount = VarIntUtil.readVarInt(buf);
                assert bufWordCount == 0;
            }
            // dummy operation
            storage = EMPTY_LONG_ARRAY;
        } else {
            entryMask = (1L << paletteStorageBits) - 1L;
            // calculate size of storage data
            valuesPerWord = (char) (Long.SIZE / paletteStorageBits);
            int wordCount = (STORAGE_SIZE_3D + valuesPerWord - 1) / valuesPerWord;
            if (storageLength) {
                // verify storage length in buffer
                int bufWordCount = VarIntUtil.readVarInt(buf);
                assert bufWordCount == wordCount;
            }
            // read storage array
            storage = new long[wordCount];
            for (int i = 0; i < wordCount; i++) {
                storage[i] = buf.readLong();
            }
        }

        // determine whether we need to resize the storage
        assert newPaletteBits >= paletteStorageBits;
        boolean resize = paletteStorageBits != newPaletteBits;

        int newValuesPerWord;
        long[] newStorage;
        if (!resize) {
            // nothing changed, no need to mess with the storage
            newValuesPerWord = valuesPerWord;
            newStorage = storage;
        } else {
            // create new empty storage array
            newValuesPerWord = (char) (Long.SIZE / newPaletteBits);
            int newWordCount = (STORAGE_SIZE_3D + newValuesPerWord - 1) / newValuesPerWord;
            newStorage = new long[newWordCount];
        }

        ReplacementStrategy strategy = this.strategy.construct(presetCount);
        for (int y = 0; y < STORAGE_SIZE; y++) {
            // tell our replacement to advance into the next layer only if actually needed
            boolean advanced = false;
            // as the storage array index is structured as Y-X-Z and we don't care about
            // specific X/Z coordinates, we can just iterate through the entire X-Z "slice"
            for (int xz = 0; xz < STORAGE_SIZE_2D; xz++) {
                int blockIndex = storageIndex(xz, y);
                int wordIndex;
                int bitIndex;
                long word;
                int value;
                if (paletteStorageBits != 0) {
                    // inspired by https://github.com/Minestom/Minestom/blob/42e0d212660d0aa307ea5fb731f22aa4e8843674/src/main/java/net/minestom/server/instance/palette/Palettes.java#L50-L57
                    // licensed under the terms of the apache license 2.0
                    wordIndex = blockIndex / valuesPerWord;
                    bitIndex = (blockIndex - wordIndex * valuesPerWord) * paletteStorageBits;
                    word = storage[wordIndex];
                    value = (int) ((word >> bitIndex) & entryMask);
                } else {
                    wordIndex = 0;
                    bitIndex = 0;
                    word = 0L;
                    value = 0;
                }

                // check if this value needs to be obfuscated
                int newValue;
                if (obfuscatedPalette.get(value)) {
                    // advance if needed
                    if (!advanced) {
                        advanced = true;
                        strategy.advance();
                    }
                    // replace value with value from strategy, after passing through palette
                    newValue = presetPalette[strategy.get()];
                    // check if we need to write here; if we need to resize, our new value will get written
                    // anyway; if the new value is the same as the old value, we can just skip handling
                    if (!resize && newValue != value) {
                        storage[wordIndex] = word
                                // remove bits from word at position of value
                                & ~(entryMask << bitIndex)
                                // write bits of new value at position
                                | (long) newValue << bitIndex;
                        continue; // skip check below
                    }
                } else {
                    // no obfuscation needed, passthrough
                    newValue = value;
                }

                // check if we need to resize
                if (resize) {
                    // set the new value into the correct word in the new storage array
                    int newWordIndex = blockIndex / newValuesPerWord;
                    int newBitIndex = (blockIndex - newWordIndex * newValuesPerWord) * newPaletteBits;
                    // we don't need to remove the previous bits, as the new storage
                    // array has no values before we write to it
                    newStorage[newWordIndex] |= (long) newValue << newBitIndex;
                }
            }
        }

        // after we've replaced data in the storage array, write all our changes to the buffer

        // reset reader index to start, the reader index always has
        // to be smaller than or equals to the writer index
        buf.readerIndex(readerIndex);

        // rewrite palette if changed
        if (newPalette != null) {
            buf.writerIndex(readerIndex);
            buf.writeByte(newPaletteBits); // palette bits
            // write palette data
            switch (newPaletteBits) {
                // single value palette
                case 0 -> VarIntUtil.writeVarInt(buf, newPalette[0]);
                // linear palette/hashmap palette
                case 1, 2, 3, 4, 5, 6, 7, 8 -> {
                    int newPaletteSize = newPalette.length;
                    VarIntUtil.writeVarInt(buf, newPaletteSize);
                    for (int i = 0; i < newPaletteSize; i++) {
                        VarIntUtil.writeVarInt(buf, newPalette[i]);
                    }
                }
                // global palette (nothing to do)
            }

            // immediately after this, we'll write the new storage array,
            // so the writer index is already positioned correctly
        } else {
            // if we didn't need to rewrite the palette, write
            // the storage array at the correct position in the buffer
            buf.writerIndex(storageIndex);
        }

        // write new storage array (or rewrite old storage array)
        int newStorageSize = newStorage.length;
        if (storageLength) {
            VarIntUtil.writeVarInt(buf, newStorageSize);
        }
        for (int i = 0; i < newStorageSize; i++) {
            buf.writeLong(newStorage[i]);
        }

        // ... and we're done!
    }
}
