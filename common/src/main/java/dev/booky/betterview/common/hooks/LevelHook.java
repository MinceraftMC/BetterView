package dev.booky.betterview.common.hooks;
// Created by booky10 in BetterView (14:08 03.06.2025)

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.booky.betterview.api.BvLevel;
import dev.booky.betterview.api.BvPlayer;
import dev.booky.betterview.common.BetterViewManager;
import dev.booky.betterview.common.ChunkCacheEntry;
import dev.booky.betterview.common.config.BvLevelConfig;
import dev.booky.betterview.common.util.ChunkTagResult;
import dev.booky.betterview.common.util.McChunkPos;
import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@NullMarked
public interface LevelHook extends BvLevel {

    BetterViewManager getManager();

    CompletableFuture<@Nullable ByteBuf> getCachedChunkBuf(McChunkPos chunkPos);

    CompletableFuture<@Nullable ChunkTagResult> readChunk(McChunkPos chunkPos);

    CompletableFuture<ByteBuf> loadChunk(int chunkX, int chunkZ);

    boolean checkChunkGeneration();

    void resetChunkGeneration();

    ByteBuf getEmptyChunkBuf(McChunkPos chunkPos);

    boolean isVoidWorld();

    Object dimension();

    BvLevelConfig getConfig();

    LoadingCache<McChunkPos, ChunkCacheEntry> getChunkCache();

    @Override
    default void invalidateChunkCache() {
        this.getChunkCache().invalidateAll();
    }

    @Override
    default void invalidateChunkCache(int chunkX, int chunkZ) {
        this.getChunkCache().invalidate(new McChunkPos(chunkX, chunkZ));
    }

    @Override
    default Set<BvPlayer> getPlayers() {
        Set<BvPlayer> players = new HashSet<>();
        for (PlayerHook player : this.getManager().getPlayers()) {
            if (player.getLevel() == this) {
                players.add(player.getBvPlayer());
            }
        }
        return Collections.unmodifiableSet(players);
    }
}
