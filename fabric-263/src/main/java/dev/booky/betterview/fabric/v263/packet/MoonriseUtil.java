package dev.booky.betterview.fabric.v263.packet;
// Created by booky10 in BetterView (4:58 PM 10.04.2026)

import dev.booky.betterview.common.util.McChunkPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

@NullMarked
public final class MoonriseUtil {

    public static boolean INSTALLED = Util.make(() -> {
        try {
            Class.forName("ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    });

    public static final ChunkStatus TARGET_STATUS = INSTALLED ? ChunkStatus.LIGHT : ChunkStatus.FULL;

    private MoonriseUtil() {
    }

    public static int getTotalLightSections(ServerLevel level) {
        return level.getMaxSectionY() - level.getMinSectionY() + 3;
    }

    public static CompletableFuture<@Nullable ChunkAccess> getLoadedChunk(ServerLevel level, McChunkPos chunkPos) {
        if (!INSTALLED) {
            return CompletableFuture.supplyAsync(() -> {
                ChunkHolder chunk = level.getChunkSource().getVisibleChunkIfPresent(chunkPos.getKey());
                return chunk == null ? null : chunk.getChunkIfPresent(TARGET_STATUS);
            }, level.getChunkSource().mainThreadProcessor);
        }

        return CompletableFuture.completedFuture(MoonriseHooks.getLoadedChunk(level, chunkPos, TARGET_STATUS));
    }

    public static CompletableFuture<ChunkAccess> getGeneratedChunk(ServerLevel level, int chunkX, int chunkZ) {
        if (!INSTALLED) {
            return generateVanillaAsync(level, chunkX, chunkZ);
        }
        return MoonriseHooks.getGeneratedChunk(level, chunkX, chunkZ);
    }

    /// Off-main getChunkFuture (no managedBlock). Do not remove the hold ticket
    /// early or vanilla Forget races the BV send.
    ///
    /// Ticket choice (26.3 / 1.21.2+): ENDER_PEARL. 40 ticks, loads+simulates, no
    /// persist. Added in 1.21.2. On 1.21.1 use TicketType.create("betterview", cmp)
    /// (timeout 0) instead.
    private static CompletableFuture<ChunkAccess> generateVanillaAsync(ServerLevel level, int chunkX, int chunkZ) {
        ServerChunkCache source = level.getChunkSource();
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        return CompletableFuture
                .runAsync(() -> source.addTicketWithRadius(TicketType.ENDER_PEARL, pos, 0), source.mainThreadProcessor)
                .thenComposeAsync(__ -> source.getChunkFuture(chunkX, chunkZ, ChunkStatus.FULL, true),
                        Util.backgroundExecutor())
                .thenApply(result -> {
                    ChunkAccess chunk = result == null ? null : result.orElse(null);
                    if (chunk == null) {
                        CompletableFuture.runAsync(
                                () -> source.removeTicketWithRadius(TicketType.ENDER_PEARL, pos, 0),
                                source.mainThreadProcessor);
                        throw new IllegalStateException("Failed to generate chunk at " + pos);
                    }
                    return chunk;
                });
    }

    public static int getSendViewDistance(ServerPlayer player) {
        if (!INSTALLED) {
            return player.level().getChunkSource().chunkMap.getPlayerViewDistance(player);
        }
        return MoonriseHooks.getSendViewDistance(player);
    }
}
