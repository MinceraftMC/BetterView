package dev.booky.betterview.api;
// Created by booky10 in BetterView (5:50 PM 09.04.2026)

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum ChunkLifecycle {

    /**
     * Means the player doesn't know about this chunk as
     * neither BetterView nor the server has sent it to them.
     */
    UNLOADED(false, false),
    /**
     * The player knows about this chunk as the server has sent it to them.
     */
    SERVER_LOADED(true, false),

    /**
     * The chunk is currently in the process of being loaded
     * by BetterView off-thread (e.g. currently being generated
     * or read from disk).
     */
    BV_QUEUED(false, true),
    /**
     * The player knows about this chunk as BetterVie has sent it to them.
     */
    BV_LOADED(true, true),
    ;

    private final boolean loaded;
    private final boolean owned;

    ChunkLifecycle(boolean loaded, boolean owned) {
        this.loaded = loaded;
        this.owned = owned;
    }

    /**
     * @return whether the player has received this chunk.
     */
    public boolean isLoaded() {
        return this.loaded;
    }

    /**
     * @return whether this chunk is currently solely managed by BetterView.
     */
    public boolean isOwned() {
        return this.owned;
    }
}
