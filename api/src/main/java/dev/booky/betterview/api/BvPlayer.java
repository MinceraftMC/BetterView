package dev.booky.betterview.api;
// Created by booky10 in BetterView (5:46 PM 09.04.2026)

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;

/**
 * Mostly thread-unsafe API for accessing data stored about
 * a player which is managed by BetterView.
 * <p>
 * Please see each methods individual Javadoc for more info on thread-safety.
 * Methods which are marked as thread unsafe will need to be run using the executor
 * returned by {@link #getExecutor()}.
 */
@NullMarked
public interface BvPlayer {

    /**
     * @return the executor which will need to be used to safely access
     * other methods of this {@link BvPlayer}.
     */
    @Contract(pure = true)
    BvExecutor getExecutor();

    /**
     * @return whether this player is currently ticked by BetterView (e.g.
     * false when entering a dimension which is disabled in BetterView's config).
     */
    @Contract(pure = true)
    boolean isActive();

    /**
     * @return the level the player is currently in.
     */
    @Contract(pure = true)
    BvLevel getLevel();

    /**
     * Not thread-safe.
     *
     * @return the lifecycle of the specified chunk position.
     */
    @Contract(pure = true)
    ChunkLifecycle getChunkLifecycle(int chunkX, int chunkZ);

    /**
     * Not thread-safe.
     * <p>
     * Queues the specified chunk for resending. This may or may not cause the current
     * chunk sending queue to be re-ordered.
     * <p>
     * Please note that this method does not invalidate the per-dimension chunk cache,
     * so the chunk may still be outdated after a refresh. See {@link BvLevel#invalidateChunkCache(int, int)}
     * for invalidating the cache of a specific chunk.
     *
     * @throws IllegalStateException if the chunk is currently NOT managed
     *                               by BetterView (see {@link ChunkLifecycle#isOwned()}).
     */
    void refreshChunk(int chunkX, int chunkZ) throws IllegalStateException;
}
