package dev.booky.betterview.common.hooks;
// Created by booky10 in BetterView (14:08 03.06.2025)

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@NullMarked
public interface BetterViewHook {

    long getNanosPerServerTick();

    LevelHook constructLevel(String worldName);

    @Nullable
    PlayerHook constructPlayer(UUID playerId);
}
