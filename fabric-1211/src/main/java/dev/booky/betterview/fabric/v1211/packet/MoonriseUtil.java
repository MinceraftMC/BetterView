package dev.booky.betterview.fabric.v1211.packet;
// Created by booky10 in BetterView (4:58 PM 10.04.2026)

import ca.spottedleaf.moonrise.common.util.ChunkSystem;
import ca.spottedleaf.moonrise.libs.ca.spottedleaf.concurrentutil.util.Priority;
import ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import dev.booky.betterview.common.util.McChunkPos;
import net.minecraft.Util;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    private MoonriseUtil() {
    }

    public static int getTotalLightSections(ServerLevel level) {
        return level.getMaxSection() - level.getMinSection() + 3;
    }

    public static CompletableFuture<@Nullable ChunkAccess> getLoadedChunk(ServerLevel level, McChunkPos chunkPos) {
        if (!INSTALLED) {
            return CompletableFuture.supplyAsync(() -> {
                ChunkHolder chunk = level.getChunkSource().getVisibleChunkIfPresent(chunkPos.getKey());
                return chunk == null ? null : chunk.getChunkIfPresent(ChunkStatus.FULL);
            }, level.getChunkSource().mainThreadProcessor);
        }

        NewChunkHolder holder = ((ChunkSystemServerLevel) level).moonrise$getChunkTaskScheduler().chunkHolderManager.getChunkHolder(chunkPos.getKey());
        if (holder == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.completedFuture(holder.getChunkIfPresent(ChunkStatus.LIGHT));
    }

    public static CompletableFuture<ChunkAccess> getGeneratedChunk(ServerLevel level, int chunkX, int chunkZ) {
        if (!MoonriseUtil.INSTALLED) {
            return CompletableFuture.supplyAsync(() -> level.getChunkSource().getChunk(
                    chunkX, chunkZ, ChunkStatus.FULL, true), level.getChunkSource().mainThreadProcessor);
        }
        CompletableFuture<ChunkAccess> future = new CompletableFuture<>();
        ChunkSystem.scheduleChunkLoad(level, chunkX, chunkZ, true,
                ChunkStatus.LIGHT, true, Priority.LOW,
                future::complete);
        return future;
    }

    public static int getSendViewDistance(ServerPlayer player) {
        if (!MoonriseUtil.INSTALLED) {
            return player.serverLevel().getChunkSource().chunkMap.getPlayerViewDistance(player);
        }
        return ChunkSystem.getSendViewDistance(player);
    }
}
