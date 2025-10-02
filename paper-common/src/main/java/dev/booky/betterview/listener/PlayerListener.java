package dev.booky.betterview.listener;
// Created by booky10 in BetterView (22:34 03.06.2025)

import dev.booky.betterview.common.BetterViewManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlayerListener implements Listener {

    private final BetterViewManager manager;

    public PlayerListener(BetterViewManager manager) {
        this.manager = manager;
        // initialize players which are already online
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.manager.getPlayerOrNull(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        this.manager.getPlayerOrNull(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        this.manager.unregisterPlayer(event.getPlayer().getUniqueId());
    }
}
