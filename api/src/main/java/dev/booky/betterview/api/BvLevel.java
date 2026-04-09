package dev.booky.betterview.api;
// Created by booky10 in BetterView (5:58 PM 09.04.2026)

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;

import java.util.Set;

/**
 * Exposes thread-safe API for accessing resources managed by BetterView
 * in relation to dimensions (e.g. chunk cache).
 */
@NullMarked
public interface BvLevel {

    /**
     * @return the name of this dimension
     */
    @Contract(pure = true)
    Key getName();

    /**
     * Invalidates the chunk cache for the entire dimension.
     */
    void invalidateChunkCache();

    /**
     * Invalidates the chunk cache for the specified chunk position.
     */
    void invalidateChunkCache(int chunkX, int chunkZ);

    /**
     * @return all players which are in this dimension.
     */
    @Contract(pure = true)
    @Unmodifiable
    Set<BvPlayer> getPlayers();
}
