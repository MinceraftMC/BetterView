package dev.booky.betterview.api;
// Created by booky10 in BetterView (5:45 PM 09.04.2026)

import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Exposes a thread-safe API for getting
 * player states and dimension states managed by BetterView.
 */
@NullMarked
public interface BvManager {

    /**
     * @return whether BetterView has encountered a player with the specified dimension name before.
     */
    boolean hasLevel(Key dimensionName);

    /**
     * This method will always return a level instance and may create a new one if none exists.
     *
     * @param dimensionName the namespaced name of the level (e.g. "minecraft:overworld")
     * @return the level managed by BetterView.
     * @throws IllegalArgumentException if the level can't be found
     */
    BvLevel getLevel(Key dimensionName) throws IllegalArgumentException;

    /**
     * This method will always return a player instance if the player is currently connected
     * to the platform and may create a new one if the player doesn't exist yet.
     *
     * @param playerId the {@link UUID} of the Minecraft player profile.
     * @return the player managed by BetterView or null if the player doesn't exist on the platform.
     */
    @Nullable BvPlayer getPlayerOrNull(UUID playerId);

    /**
     * @throws IllegalArgumentException if {@link #getPlayerOrNull(UUID)} returns null.
     * @see #getPlayerOrNull(UUID)
     */
    default BvPlayer getPlayer(UUID playerId) throws IllegalArgumentException {
        BvPlayer player = this.getPlayerOrNull(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Can't find player with id " + playerId);
        }
        return player;
    }
}
