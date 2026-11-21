package dev.booky.betterview.fabric.v263.packet;
// isolate Moonrise chunk-system types

import ca.spottedleaf.moonrise.common.PlatformHooks;
import ca.spottedleaf.moonrise.libs.ca.spottedleaf.concurrentutil.util.Priority;
import ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import dev.booky.betterview.common.util.McChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/// Only referenced when MoonriseUtil.INSTALLED is true.
@NullMarked
public final class MoonriseHooks {
    public static @Nullable ChunkAccess getLoadedChunk(ServerLevel level, McChunkPos chunkPos, ChunkStatus status) {
        NewChunkHolder holder = ((ChunkSystemServerLevel) level).moonrise$getChunkTaskScheduler().chunkHolderManager
                .getChunkHolder(chunkPos.getKey());
        if (holder == null) {
            return null;
        }
        return holder.getChunkIfPresent(status);
    }

    public static CompletableFuture<ChunkAccess> getGeneratedChunk(ServerLevel level, int chunkX, int chunkZ) {
        CompletableFuture<ChunkAccess> future = new CompletableFuture<>();
        PlatformHooks.get().scheduleChunkLoad(level, chunkX, chunkZ, true,
                ChunkStatus.FULL, true, Priority.LOW,
                future::complete);
        return future;
    }

    public static int getSendViewDistance(ServerPlayer player) {
        return PlatformHooks.get().getSendViewDistance(player);
    }
}
