package dev.booky.betterview.platform;
// Created by booky10 in BetterView (15:42 03.06.2025)

import dev.booky.betterview.common.BetterViewManager;
import dev.booky.betterview.common.hooks.BetterViewHook;
import dev.booky.betterview.common.hooks.LevelHook;
import dev.booky.betterview.common.hooks.PlayerHook;
import dev.booky.betterview.nms.PaperNmsInterface;
import io.netty.channel.Channel;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@NullMarked
public class PaperBetterView implements BetterViewHook {

    private static final Logger LOGGER = LoggerFactory.getLogger("BetterView");

    private final BetterViewManager manager;

    public PaperBetterView(BetterViewManager manager) {
        this.manager = manager;
    }

    @Override
    public long getNanosPerServerTick() {
        return PaperNmsInterface.SERVICE.getNanosPerServerTick();
    }

    @SuppressWarnings("PatternValidation")
    @Override
    public LevelHook constructLevel(String worldName) {
        World world = Bukkit.getWorld(Key.key(worldName));
        if (world == null) {
            throw new IllegalStateException("Can't find level with name " + worldName);
        }
        return new PaperLevel(this.manager, world);
    }

    @Override
    public @Nullable PlayerHook constructPlayer(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            throw new IllegalStateException("Can't find player with uuid " + playerId);
        }
        Channel channel = PaperNmsInterface.SERVICE.getNettyChannel(player);
        if (!PaperNmsInterface.SERVICE.isInjected(channel)) {
            if (!PaperNmsInterface.SERVICE.isFakeChannel(channel)) {
                LOGGER.warn("Failed to inject into player {} (channel {})",
                        playerId, PaperNmsInterface.SERVICE.getNettyChannel(player));
            }
            return null;
        }
        return new PaperPlayer(this.manager, player);
    }
}
