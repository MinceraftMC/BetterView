package dev.booky.betterview.fabric.v1215.packet;
// Created by booky10 in BetterView (21:00 03.06.2025)

import ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray;
import dev.booky.betterview.fabric.v1215.mixin.accessor.SWMRNibbleArrayAccessor;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LightEngine;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

@NullMarked
public final class LightWriter {

    private LightWriter() {
    }

    @Contract("_, _, _, false -> !null")
    public static byte @Nullable [][] convertLightToBytes(ServerLevel level, LayerLightEventListener layer, ChunkPos pos, boolean allowEmpty) {
        int layerCount = MoonriseUtil.getTotalLightSections(level);
        byte[][] layers = new byte[layerCount][];
        if (layer == LayerLightEventListener.DummyLightLayerEventListener.INSTANCE) {
            return !allowEmpty ? layers : null;
        }
        boolean nonEmpty = false;
        // fast path if this is a vanilla engine (prevents allocating
        // SectionPos by directly accessing the underlying storage)
        if (layer instanceof LightEngine<?, ?> engine) {
            long sectionPosBase = ((pos.x & 0x3FFFFFL) << (64 - 22)) | ((pos.z & 0x3FFFFFL) << (64 - 22 - 22));
            for (int i = 0; i < layerCount; i++) {
                DataLayer data = engine.storage.getDataLayerData(sectionPosBase | (i & 0xFFFFFL));
                if (data != null) {
                    layers[i] = data.getData();
                    nonEmpty = true;
                }
            }
        } else {
            for (int i = 0; i < layerCount; i++) {
                DataLayer data = layer.getDataLayerData(SectionPos.of(pos, i));
                if (data != null) {
                    layers[i] = data.getData();
                    nonEmpty = true;
                }
            }
        }
        return nonEmpty || !allowEmpty ? layers : null;
    }

    @Contract("_, false -> !null")
    public static byte @Nullable [][] convertStarlightToBytes(SWMRNibbleArray[] layers, boolean allowEmpty) {
        int layerCount = layers.length;
        byte[][] byteLayers = new byte[layerCount][];
        boolean converted = false;
        for (int i = 0; i < layerCount; i++) {
            SWMRNibbleArray layer = layers[i];
            if (layer.isInitialisedVisible()) {
                // bypass cloning by accessing underlying field
                byteLayers[i] = ((SWMRNibbleArrayAccessor) (Object) layer).getStorageVisible();
                converted = true;
            }
        }
        return converted || !allowEmpty ? byteLayers : null;
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
