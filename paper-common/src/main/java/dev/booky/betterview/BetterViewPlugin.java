package dev.booky.betterview;
// Created by booky10 in BetterView (15:40 03.06.2025)

import dev.booky.betterview.common.BetterViewManager;
import dev.booky.betterview.listener.LevelListener;
import dev.booky.betterview.listener.PlayerListener;
import dev.booky.betterview.nms.PaperNmsInterface;
import dev.booky.betterview.platform.PaperBetterView;
import net.kyori.adventure.util.Ticks;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;

@NullMarked
public class BetterViewPlugin extends JavaPlugin {

    private final BetterViewManager manager;
    private @MonotonicNonNull NamespacedKey listenerKey;

    public BetterViewPlugin() {
        Path configPath = this.getDataPath().resolve("config.yml");
        this.manager = new BetterViewManager(PaperBetterView::new, configPath);
    }

    @Override
    public void onLoad() {
        this.listenerKey = new NamespacedKey(this, "packets");

        // see https://bstats.org/plugin/bukkit/BetterView/26105
        new Metrics(this, 26105);
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new LevelListener(this.manager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this.manager), this);

        // inject packet handling
        PaperNmsInterface.SERVICE.injectPacketHandler(this.manager, this.listenerKey);

        // run task after server has finished starting
        Bukkit.getGlobalRegionScheduler().run(this, __ -> this.manager.onPostLoad());

        // start ticking after one fourth of a second so we know for sure our post-load has already happened
        // this dynamically adjusts with the tickrate the server is set to and is not time-based
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, __ -> this.manager.runTick(),
                Ticks.TICKS_PER_SECOND / 4, 1L);
    }

    @Override
    public void onDisable() {
        // uninject packet handling
        PaperNmsInterface.SERVICE.uninjectPacketHandler(this.listenerKey);
    }
}
