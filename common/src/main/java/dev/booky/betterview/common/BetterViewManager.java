package dev.booky.betterview.common;
// Created by booky10 in BetterView (15:19 03.06.2025)

import dev.booky.betterview.common.BetterViewPlayer.ChunkQueueEntry;
import dev.booky.betterview.common.config.BvConfig;
import dev.booky.betterview.common.config.BvLevelConfig;
import dev.booky.betterview.common.config.loading.ConfigurateLoader;
import dev.booky.betterview.common.hooks.BetterViewHook;
import dev.booky.betterview.common.hooks.LevelHook;
import dev.booky.betterview.common.hooks.PlayerHook;
import dev.booky.betterview.common.util.McChunkPos;
import io.leangen.geantyref.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.util.NettyRuntime;
import io.netty.util.internal.SystemPropertyUtil;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

// BV processing logic is done asynchronously on the netty threads
// as this ensures dimension switches don't cause chunks to end up
// in the wrong dimension because of race conditions
@NullMarked
public final class BetterViewManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("BetterView");

    // determines how much of a tick this plugin is allowed to process at maximum
    static final int TICK_LENGTH_DIVISOR = 2;

    private final AtomicInteger generatedChunks = new AtomicInteger(0);
    private final BetterViewHook hook;

    private final Map<String, LevelHook> levels = new HashMap<>();
    private final Map<UUID, PlayerHook> players = new HashMap<>();

    private final Path configPath;
    private BvConfig config;

    public BetterViewManager(Function<BetterViewManager, BetterViewHook> hookConstructor, Path configPath) {
        this.hook = hookConstructor.apply(this);
        this.configPath = configPath;
        this.config = this.loadConfig();
        this.saveConfig();
    }

    private BvConfig loadConfig() {
        return ConfigurateLoader.loadYaml(BvConfig.SERIALIZERS, this.configPath,
                new TypeToken<BvConfig>() {}, BvConfig::new);
    }

    private void saveConfig() {
        ConfigurateLoader.saveYaml(BvConfig.SERIALIZERS, this.configPath,
                new TypeToken<BvConfig>() {}, this.config);
    }

    public void onPostLoad() {
        // reload config and populate dimensions
        this.config = this.loadConfig();
        for (LevelHook level : this.levels.values()) {
            this.config.getLevelConfig(level.getName());
        }
        this.saveConfig();
    }

    // called on main thread
    public void runTick() {
        BetterViewHook hook = this.hook;
        if (!this.config.getGlobalConfig().isEnabled()) {
            return; // disabled globally
        }

        List<? extends PlayerHook> players = new ArrayList<>(this.players.values());
        if (players.isEmpty()) {
            return; // no players online
        }

        // reset chunk generation counters
        this.generatedChunks.set(0);
        for (LevelHook level : this.levels.values()) {
            level.resetChunkGeneration();
        }

        // shuffle players to randomize who gets more/less time this tick
        Collections.shuffle(players);

        // based on how many netty threads exist and how often the server ticks,
        // calculate how much nanos each player gets to tick
        int nettyThreadCount = Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));
        long tickLengthNanos = hook.getNanosPerServerTick() / TICK_LENGTH_DIVISOR;
        long maxTimePerPlayerNanos = (nettyThreadCount * tickLengthNanos) / Math.max(nettyThreadCount, players.size());

        for (PlayerHook player : players) {
            LevelHook level = player.getLevel();
            McChunkPos chunkPos = player.getChunkPos();
            // switch to netty threads for ticking
            player.getNettyChannel().eventLoop().execute(() -> {
                try {
                    long deadline = System.nanoTime() + maxTimePerPlayerNanos;
                    this.tickPlayer(player, level, chunkPos, deadline);
                } catch (Throwable throwable) {
                    LOGGER.error("Error while ticking player {} in level {}", player, level, throwable);
                }
            });
        }
    }

    private void tickPlayer(PlayerHook player, LevelHook level, McChunkPos chunkPos, long deadline) {
        BetterViewPlayer bv = player.getBvPlayer();

        // tick player movement
        bv.move(level, chunkPos);

        if (!bv.preTick()) {
            return; // don't tick player
        }

        // start processing chunks (process at least once)
        int chunksPerTick = this.config.getGlobalConfig().getChunkSendLimit();
        int chunkQueueSize = level.getConfig().getChunkQueueSize();
        do {
            // check if any chunks are built and ready for sending
            bv.chunkQueue.removeIf(bv::checkQueueEntry);

            if (bv.chunkQueue.size() >= chunkQueueSize) {
                break; // limit how many chunks a player can queue at once
            }

            McChunkPos nextChunk = bv.pollChunkPos();
            if (nextChunk == null) {
                break; // nothing left to process, player can see everything
            }

            // start building chunk queue and add to processing queue
            CompletableFuture<@Nullable ByteBuf> future = level.getChunkCache().get(nextChunk).get();
            bv.chunkQueue.add(new ChunkQueueEntry(nextChunk, future));

            // check if a limit has been reached
            if (chunksPerTick-- <= 0) {
                break;
            }
        } while (deadline > System.nanoTime());
    }

    public boolean checkChunkGeneration() {
        return this.generatedChunks.getAndIncrement() <= this.config.getGlobalConfig().getChunkGenerationLimit();
    }

    public LevelHook getLevel(Key worldName) {
        return this.levels.computeIfAbsent(worldName.asString(), this.hook::constructLevel);
    }

    public void unregisterLevel(Key worldName) {
        this.levels.remove(worldName.asString());
    }

    public @Nullable PlayerHook getPlayerOrNull(UUID playerId) {
        return this.players.computeIfAbsent(playerId, this.hook::constructPlayer);
    }

    public PlayerHook getPlayer(UUID playerId) {
        PlayerHook player = this.getPlayerOrNull(playerId);
        if (player == null) {
            throw new IllegalStateException("Can't construct player " + playerId);
        }
        return player;
    }

    public void unregisterPlayer(UUID playerId) {
        this.players.remove(playerId);
    }

    public BvLevelConfig getConfig(Key worldName) {
        return this.config.getLevelConfig(worldName);
    }

    public BvConfig getConfig() {
        return this.config;
    }
}
