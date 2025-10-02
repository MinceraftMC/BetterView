package dev.booky.betterview.nms.v1215;
// Created by booky10 in BetterView (21:00 03.06.2025)

import ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray;
import com.destroystokyo.paper.util.SneakyThrow;
import io.netty.buffer.ByteBuf;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

@NullMarked
public final class LightWriter {

    private static final MethodHandle GET_STORAGE_VISIBLE = Util.make(() -> {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(SWMRNibbleArray.class, MethodHandles.lookup());
            return lookup.findGetter(SWMRNibbleArray.class, "storageVisible", byte[].class);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Error while looking for starlight data layer storage getter");
        }
    });

    private LightWriter() {
    }

    @Contract("_, false -> !null")
    public static byte @Nullable [][] convertStarlightToBytes(SWMRNibbleArray[] layers, boolean allowEmpty) {
        try {
            int layerCount = layers.length;
            byte[][] byteLayers = new byte[layerCount][];
            boolean converted = false;
            for (int i = 0; i < layerCount; i++) {
                SWMRNibbleArray layer = layers[i];
                if (layer.isInitialisedVisible()) {
                    // bypass cloning by accessing underlying field
                    byteLayers[i] = (byte[]) GET_STORAGE_VISIBLE.invoke(layer);
                    converted = true;
                }
            }
            return converted || !allowEmpty ? byteLayers : null;
        } catch (Throwable throwable) {
            SneakyThrow.sneaky(throwable);
            throw new AssertionError();
        }
    }

    public static void writeLightData(ByteBuf buf, byte[][] blockLight, byte @Nullable [][] skyLight) {
        if (skyLight == null) {
            writeNoSkyLightData(buf, blockLight);
            return;
        }

        // generate light data
        List<byte[]> skyData = new ArrayList<>(skyLight.length);
        BitSet notSkyEmpty = new BitSet();
        BitSet skyEmpty = new BitSet();

        int blockLightLen = blockLight.length;
        List<byte[]> blockData = new ArrayList<>(blockLightLen);
        BitSet notBlockEmpty = new BitSet();
        BitSet blockEmpty = new BitSet();

        for (int indexY = 0; indexY < blockLightLen; indexY++) {
            byte[] sky = skyLight[indexY];
            if (sky == null) {
                skyEmpty.set(indexY);
            } else {
                notSkyEmpty.set(indexY);
                skyData.add(sky);
            }
            byte[] block = blockLight[indexY];
            if (block == null) {
                blockEmpty.set(indexY);
            } else {
                notBlockEmpty.set(indexY);
                blockData.add(block);
            }
        }

        // write light data
        writeBitSet(buf, notSkyEmpty.toLongArray());
        writeBitSet(buf, notBlockEmpty.toLongArray());
        writeBitSet(buf, skyEmpty.toLongArray());
        writeBitSet(buf, blockEmpty.toLongArray());
        writeByteArrayList(buf, skyData);
        writeByteArrayList(buf, blockData);
    }

    private static void writeNoSkyLightData(ByteBuf buf, byte[][] blockLight) {
        // generate light data
        int blockLightLen = blockLight.length;
        List<byte[]> blockData = new ArrayList<>(blockLightLen);
        BitSet notBlockEmpty = new BitSet();
        BitSet blockEmpty = new BitSet();

        for (int indexY = 0; indexY < blockLightLen; indexY++) {
            byte[] block = blockLight[indexY];
            if (block == null) {
                blockEmpty.set(indexY);
            } else {
                notBlockEmpty.set(indexY);
                blockData.add(block);
            }
        }

        // write light data
        buf.writeByte(0); // sky light y mask length
        writeBitSet(buf, notBlockEmpty.toLongArray());
        buf.writeByte(0); // sky light empty y mask length
        writeBitSet(buf, blockEmpty.toLongArray());
        buf.writeByte(0); // sky light data length
        writeByteArrayList(buf, blockData);
    }

    private static void writeBitSet(ByteBuf buf, long[] set) {
        int len = set.length;
        VarInt.write(buf, len);
        for (int i = 0; i < len; ++i) {
            buf.writeLong(set[i]);
        }
    }

    private static void writeByteArrayList(ByteBuf buf, List<byte[]> list) {
        int len = list.size();
        if (len == 0) {
            buf.writeByte(0); // varint
        } else if (len == 1) {
            buf.writeByte(1); // varint
            FriendlyByteBuf.writeByteArray(buf, list.getFirst());
        } else {
            VarInt.write(buf, len);
            for (int i = 0; i < len; ++i) {
                FriendlyByteBuf.writeByteArray(buf, list.get(i));
            }
        }
    }
}
