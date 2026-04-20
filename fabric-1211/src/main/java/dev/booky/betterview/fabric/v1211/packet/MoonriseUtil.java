package dev.booky.betterview.fabric.v1211.packet;
// Created by booky10 in BetterView (4:58 PM 10.04.2026)

import ca.spottedleaf.moonrise.common.util.ChunkSystem;
import ca.spottedleaf.moonrise.libs.ca.spottedleaf.concurrentutil.util.Priority;
import ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import dev.booky.betterview.common.util.McChunkPos;
import net.minecraft.Util;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
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

    /// used for batching chunk generation tasks on the main thread as vanilla has horrible
    /// performance if only generating one chunk at a time
    private static final Map<Long, CompletableFuture<ChunkAccess>> GENERATION_QUEUE = new HashMap<>();

    public static final ChunkStatus TARGET_STATUS = INSTALLED ? ChunkStatus.LIGHT : ChunkStatus.FULL;
    private static final int TARGET_LEVEL = ChunkLevel.byStatus(TARGET_STATUS);

    private MoonriseUtil() {
    }

    public static int getTotalLightSections(ServerLevel level) {
        return level.getMaxSection() - level.getMinSection() + 3;
    }

    public static CompletableFuture<@Nullable ChunkAccess> getLoadedChunk(ServerLevel level, McChunkPos chunkPos) {
        if (!INSTALLED) {
            return CompletableFuture.supplyAsync(() -> {
                ChunkHolder chunk = level.getChunkSource().getVisibleChunkIfPresent(chunkPos.getKey());
                return chunk == null ? null : chunk.getChunkIfPresent(TARGET_STATUS);
            }, level.getChunkSource().mainThreadProcessor);
        }

        NewChunkHolder holder = ((ChunkSystemServerLevel) level).moonrise$getChunkTaskScheduler().chunkHolderManager.getChunkHolder(chunkPos.getKey());
        if (holder == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.completedFuture(holder.getChunkIfPresent(ChunkStatus.LIGHT));
    }

    public static CompletableFuture<ChunkAccess> getGeneratedChunk(ServerLevel level, int chunkX, int chunkZ) {
        if (!INSTALLED) {
            synchronized (GENERATION_QUEUE) {
                return GENERATION_QUEUE.computeIfAbsent(ChunkPos.asLong(chunkX, chunkZ),
                        __ -> new CompletableFuture<>());
            }
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

    /// needs to be called on main thread
    public static void tickChunkGenerationQueue(ServerChunkCache chunkSource) {
        if (!INSTALLED) {
            return;
        }

        // schedule chunk generation tasks
        synchronized (GENERATION_QUEUE) {
            // add tickets for all chunks
            for (long chunkKey : GENERATION_QUEUE.keySet()) {
                ChunkPos pos = new ChunkPos(chunkKey);
                chunkSource.distanceManager.addTicket(TicketType.UNKNOWN, pos, TARGET_LEVEL, pos);
            }
            // load chunks by running ticket updates
            chunkSource.distanceManager.runAllUpdates(chunkSource.chunkMap);

            for (Map.Entry<Long, CompletableFuture<ChunkAccess>> entry : GENERATION_QUEUE.entrySet()) {
                // chunk should be loaded after adding ticket for it
                ChunkHolder chunkHolder = chunkSource.getVisibleChunkIfPresent(entry.getKey());
                if (chunkHolder == null || chunkHolder.getTicketLevel() > TARGET_LEVEL) {
                    throw new IllegalStateException("Failed to get chunk holder after loading at " + entry.getKey());
                }
                // run generation tasks in parallel
                CompletableFuture<ChunkAccess> future = entry.getValue();
                chunkHolder.scheduleChunkGenerationTask(TARGET_STATUS, chunkSource.chunkMap)
                        .thenApply(result -> result.orElseThrow(() -> new IllegalStateException("Failed to generate chunk at " + entry.getKey())))
                        .whenComplete((chunk, error) -> {
                            if (error != null) {
                                future.completeExceptionally(error);
                            } else {
                                future.complete(chunk);
                            }
                        });
            }
            GENERATION_QUEUE.clear();
        }
    }
}
