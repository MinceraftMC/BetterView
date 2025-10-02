package dev.booky.betterview.nms.v1215;
// Created by booky10 in BetterView (21:19 03.06.2025)

import ca.spottedleaf.moonrise.common.util.WorldUtil;
import ca.spottedleaf.moonrise.patches.starlight.util.SaveUtil;
import com.destroystokyo.paper.util.SneakyThrow;
import com.mojang.serialization.Codec;
import dev.booky.betterview.common.antixray.AntiXrayProcessor;
import dev.booky.betterview.nms.ReflectionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Optional;

import static dev.booky.betterview.nms.v1215.ChunkWriter.SENDABLE_HEIGHTMAP_TYPES;

@NullMarked
public final class ChunkTagTransformer {

    private static final long[][] EMPTY_LONG_2D_ARRAY = new long[0][];

    private static final MethodHandle MAKE_BIOME_CODEC_RW = ReflectionUtil.getMethod(
            SerializableChunkData.class, MethodType.methodType(Codec.class, Registry.class), 1);

    private ChunkTagTransformer() {
    }

    @SuppressWarnings("unchecked")
    private static Codec<PalettedContainer<Holder<Biome>>> makeBiomeCodecRW(Registry<Biome> registry) {
        try {
            return (Codec<PalettedContainer<Holder<Biome>>>) MAKE_BIOME_CODEC_RW.invoke(registry);
        } catch (Throwable throwable) {
            SneakyThrow.sneaky(throwable);
            throw new AssertionError();
        }
    }

    public static boolean isChunkLit(CompoundTag tag) {
        Optional<String> statusName = tag.getString("Status");
        if (statusName.isEmpty()) {
            return false; // missing data
        }
        ChunkStatus status = ChunkStatus.byName(statusName.get());
        if (!status.isOrAfter(ChunkStatus.LIGHT)) {
            return false; // not lit yet
        } else if (tag.get(SerializableChunkData.IS_LIGHT_ON_TAG) == null) {
            return false; // light isn't activated
        }
        // check whether starlight version matches
        Optional<Integer> lightVersion = tag.getInt(SaveUtil.STARLIGHT_VERSION_TAG);
        return lightVersion.isPresent() && lightVersion.get() == SaveUtil.getLightVersion();
    }

    private static boolean extractChunkData(
            ServerLevel level, CompoundTag chunkTag, ChunkPos pos,
            LevelChunkSection[] sections,
            byte[][] blockLight,
            byte @Nullable [][] skyLight
    ) {
        Registry<Biome> biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        Codec<PalettedContainer<Holder<Biome>>> biomeCodec = makeBiomeCodecRW(biomeRegistry);

        ListTag sectionTags = chunkTag.getListOrEmpty(SerializableChunkData.SECTIONS_TAG);
        int minLightSection = WorldUtil.getMinLightSection(level);

        boolean onlyAir = true;
        for (int i = 0; i < sectionTags.size(); ++i) {
            CompoundTag sectionTag = sectionTags.getCompound(i).orElseThrow();
            byte sectionY = sectionTag.getByte("Y").orElseThrow();
            int sectionIndex = level.getSectionIndexFromSectionY(sectionY);

            if (sectionIndex >= 0 && sectionIndex < sections.length) {
                BlockState[] presetBlockStates = level.chunkPacketBlockController
                        .getPresetBlockStates(level, pos, sectionY);

                PalettedContainer<BlockState> blocks;
                if (sectionTag.get("block_states") instanceof CompoundTag blockStatesTag) {
                    Codec<PalettedContainer<BlockState>> blockStateCodec;
                    if (presetBlockStates != null) {
                        blockStateCodec = PalettedContainer.codecRW(
                                Block.BLOCK_STATE_REGISTRY,
                                BlockState.CODEC,
                                PalettedContainer.Strategy.SECTION_STATES,
                                Blocks.AIR.defaultBlockState(),
                                presetBlockStates
                        );
                    } else {
                        blockStateCodec = SerializableChunkData.BLOCK_STATE_CODEC;
                    }
                    blocks = blockStateCodec.parse(NbtOps.INSTANCE, blockStatesTag).getOrThrow();
                } else {
                    blocks = new PalettedContainer<>(
                            Block.BLOCK_STATE_REGISTRY,
                            Blocks.AIR.defaultBlockState(),
                            PalettedContainer.Strategy.SECTION_STATES,
                            presetBlockStates
                    );
                }

                PalettedContainer<Holder<Biome>> biomes;
                if (sectionTag.get("biomes") instanceof CompoundTag biomesTag) {
                    biomes = biomeCodec.parse(NbtOps.INSTANCE, biomesTag).getOrThrow();
                } else {
                    biomes = new PalettedContainer<>(
                            biomeRegistry.asHolderIdMap(),
                            biomeRegistry.getOrThrow(Biomes.PLAINS),
                            PalettedContainer.Strategy.SECTION_BIOMES,
                            null
                    );
                }

                LevelChunkSection section = new LevelChunkSection(blocks, biomes);
                sections[sectionIndex] = section;

                if (!section.hasOnlyAir()) {
                    onlyAir = false;
                }
            }

            if (sectionTag.get(SerializableChunkData.BLOCK_LIGHT_TAG) instanceof ByteArrayTag lightTag) {
                blockLight[sectionY - minLightSection] = lightTag.getAsByteArray();
            }
            if (skyLight != null && sectionTag.get(SerializableChunkData.SKY_LIGHT_TAG) instanceof ByteArrayTag lightTag) {
                skyLight[sectionY - minLightSection] = lightTag.getAsByteArray();
            }
        }
        return onlyAir;
    }

    private static long[] @Nullable [] extractHeightmapsData(CompoundTag chunkTag) {
        CompoundTag heightmaps = chunkTag.getCompoundOrEmpty(SerializableChunkData.HEIGHTMAPS_TAG);
        if (heightmaps.isEmpty()) {
            return EMPTY_LONG_2D_ARRAY;
        }
        long[] @Nullable [] heightmapsData = new long[SENDABLE_HEIGHTMAP_TYPES.length][];
        for (int i = 0, len = SENDABLE_HEIGHTMAP_TYPES.length; i < len; i++) {
            String key = SENDABLE_HEIGHTMAP_TYPES[i].getSerializationKey();
            if (heightmaps.get(key) instanceof LongArrayTag tag) {
                heightmapsData[i] = tag.getAsLongArray();
            }
        }
        return heightmapsData;
    }

    public static ByteBuf transformToBytesOrEmpty(
            ServerLevel level, CompoundTag chunkTag,
            @Nullable AntiXrayProcessor antiXray, ChunkPos pos
    ) {
        // extract relevant chunk data
        LevelChunkSection[] sections = new LevelChunkSection[level.getSectionsCount()];
        byte[][] blockLight = new byte[WorldUtil.getTotalLightSections(level)][];
        byte[][] skyLight = level.dimensionType().hasSkyLight() ? new byte[blockLight.length][] : null;
        boolean onlyAir = extractChunkData(level, chunkTag, pos, sections, blockLight, skyLight);
        if (onlyAir) {
            // empty, skip writing useless packet
            return Unpooled.EMPTY_BUFFER;
        }
        long[] @Nullable [] heightmapsData = extractHeightmapsData(chunkTag);
        // delegate to chunk writing method
        return ChunkWriter.writeFull(
                pos.x, pos.z, antiXray, level.getMinSectionY(),
                heightmapsData, sections, blockLight, skyLight
        );
    }
}
