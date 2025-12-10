package dev.booky.betterview.fabric.v12111;
// Created by booky10 in BetterView (04:12 05.06.2025)

import dev.booky.betterview.common.BetterViewManager;
import dev.booky.betterview.common.hooks.BetterViewHook;
import dev.booky.betterview.common.hooks.LevelHook;
import dev.booky.betterview.common.hooks.PlayerHook;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@NullMarked
public class BetterViewMod implements BetterViewHook, ModInitializer {

    public static @MonotonicNonNull BetterViewMod INSTANCE = null;

    private @Nullable WeakReference<MinecraftServer> server = null;
    private @Nullable BetterViewManager manager;

    public BetterViewMod() {
        if (INSTANCE != null) {
            throw new IllegalStateException("Mod has already been constructed");
        }
        INSTANCE = this;
    }

    @Override
    public void onInitialize() {
        // NO-OP
    }

    public void triggerPreLoad(MinecraftServer server, Path worldDir) {
        // save server instance
        this.server = new WeakReference<>(server);
        // initialize BV manager with config inside the root world directory
        Path configPath = worldDir.resolve("betterview.yml");
        this.manager = new BetterViewManager(__ -> this, configPath);
    }

    public void triggerPostLoad(Set<ResourceKey<Level>> levelKeys) {
        BetterViewManager manager = this.getManager();
        // eagerly load all available dimensions once on startup to allow
        // population of config file - every other dimension gets lazy loaded
        for (ResourceKey<Level> levelKey : levelKeys) {
            manager.getLevel(levelKey.location());
        }
        // call post-load action
        manager.onPostLoad();
    }

    public void triggerShutdown() {
        this.server = null;
        this.manager = null;
    }

    private MinecraftServer getServer() {
        MinecraftServer server;
        if (this.server == null || (server = this.server.get()) == null) {
            throw new IllegalStateException("No MinecraftServer instance is currently running");
        }
        return server;
    }

    @Override
    public long getNanosPerServerTick() {
        return this.getServer().tickRateManager().nanosecondsPerTick();
    }

    @Override
    public LevelHook constructLevel(String worldName) {
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(worldName));
        ServerLevel level = this.getServer().getLevel(levelKey);
        if (level == null) {
            throw new IllegalArgumentException("Can't find level with name " + worldName);
        }
        return (LevelHook) level;
    }

    @Override
    public PlayerHook constructPlayer(UUID playerId) {
        ServerPlayer player = this.getServer().getPlayerList().getPlayer(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Can't find player with id " + playerId);
        }
        return (PlayerHook) player;
    }

    public BetterViewManager getManager() {
        if (this.manager == null) {
            throw new IllegalStateException("Manager not loaded");
        }
        return this.manager;
    }
}
