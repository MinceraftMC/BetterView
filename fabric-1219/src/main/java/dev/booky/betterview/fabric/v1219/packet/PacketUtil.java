package dev.booky.betterview.fabric.v1219.packet;
// Created by booky10 in BetterView (04:08 05.06.2025)

import ca.spottedleaf.moonrise.patches.starlight.chunk.StarlightChunk;
import dev.booky.betterview.common.antixray.AntiXrayProcessor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PacketUtil {

    // magic packet id values
    public static final byte FORGET_LEVEL_CHUNK_PACKET_ID = 0x25;
    public static final ByteBuf FORGET_LEVEL_CHUNK_PACKET_ID_BUF =
            Unpooled.wrappedBuffer(new byte[]{FORGET_LEVEL_CHUNK_PACKET_ID});
    public static final byte LEVEL_CHUNK_WITH_LIGHT_PACKET_ID = 0x2C;
    public static final ByteBuf LEVEL_CHUNK_WITH_LIGHT_PACKET_ID_BUF =
            Unpooled.wrappedBuffer(new byte[]{LEVEL_CHUNK_WITH_LIGHT_PACKET_ID});

    private PacketUtil() {
    }

    public static ByteBuf buildEmptyChunkData(ServerLevel level, @Nullable AntiXrayProcessor antiXray) {
        Registry<Biome> biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        Holder.Reference<Biome> biome = biomeRegistry.getOrThrow(Biomes.THE_VOID);
        EmptyLevelChunk chunk = new EmptyLevelChunk(level, ChunkPos.ZERO, biome);

        ByteBuf buf = Unpooled.buffer();
        try {
            long[] @Nullable [] heightmapsData = ChunkWriter.extractHeightmapsData(chunk);
            byte[][] blockLight = LightWriter.convertStarlightToBytes(((StarlightChunk) chunk).starlight$getBlockNibbles(), false);
            byte[][] skyLight = LightWriter.convertStarlightToBytes(((StarlightChunk) chunk).starlight$getSkyNibbles(), true);
            ChunkWriter.writeFullBody(buf, antiXray, level.getMinSectionY(),
                    heightmapsData, chunk.getSections(), blockLight, skyLight);
            return buf.retain();
        } finally {
            buf.release();
        }
    }

    public static boolean checkVoidWorld(ServerLevel level) {
        if (level.getChunkSource().getGenerator() instanceof FlatLevelSource flat) {
            return flat.settings().getLayers().stream()
                    .noneMatch(state -> state != null && !state.isAir());
        }
        return false;
    }
}
