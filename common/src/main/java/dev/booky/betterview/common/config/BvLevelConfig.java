package dev.booky.betterview.common.config;
// Created by booky10 in BetterView (14:49 03.06.2025)

import dev.booky.betterview.common.antixray.ReplacementStrategy;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.time.Duration;
import java.util.List;

@NullMarked
@ConfigSerializable
public class BvLevelConfig {

    @Comment("Whether the extended view distance is enabled for this dimension or not")
    private boolean enabled = true;
    @Comment("How many new chunks can be generated for this level in one tick")
    private int chunkGenerationLimit = 2;
    @Comment("How many chunks can be queued per player at once")
    private int chunkQueueSize = 16;
    @Comment("The maximum extended view distance for this level")
    private int viewDistance = 32;
    @Comment("""
            The cache duration for all extended chunks, after which they will be re-build
            Make sure to adjust the cache duration based on what you use your server for;
            for e.g. static lobby servers, you can use a longer cache duration than for dynamic SMP servers.
            If the cache duration is too high, chunks will display outdated content after e.g. a rejoin.""")
    private Duration cacheDuration = Duration.ofMinutes(5L);
    @Comment("""
            Configuration options for a lightweight anti-xray applied to extended chunks.
            be aware that this plugin implements a lightweight version of anti-xray, which
            doesn't check if a block is exposed to air or not. This means that every engine-mode other than `HIDE`
            will probably not look very good.""")
    private AntiXrayConfig antiXray = new AntiXrayConfig();
    @Comment("""
            Configuration options for chunk batches (per-player).
            Chunk batches are used to ensure the client/internet of players doesn't get overwhelmed by
            too many chunks being sent at once. If enabled, BetterView sends a chunk batch and waits
            for the player to acknowledge they received the full chunk batch. Only then the next batch of chunks
            will be sent.
            A side effect of this will be that players with good connectivity but high latency
            will receive chunks at a much slower rate than possible.
            Note: chunk batches may be sent across multiple ticks""")
    private BatchConfig chunkBatches = new BatchConfig();

    public boolean isEnabled() {
        return this.enabled;
    }

    public int getChunkGenerationLimit() {
        return this.chunkGenerationLimit;
    }

    public int getChunkQueueSize() {
        return this.chunkQueueSize;
    }

    public int getViewDistance() {
        return this.viewDistance;
    }

    public Duration getCacheDuration() {
        return this.cacheDuration;
    }

    public AntiXrayConfig getAntiXray() {
        return this.antiXray;
    }

    public BatchConfig getChunkBatches() {
        return this.chunkBatches;
    }

    @ConfigSerializable
    public static final class AntiXrayConfig {

        @Comment("Whether or not anti-xray is enabled for extended chunks")
        private boolean enabled = false;
        @Comment("The anti-xray engine mode, recommended to be left at \"HIDE\" as this\n"
                + "anti-xray doesn't check whether a block is exposed or not")
        private EngineMode engineMode = EngineMode.HIDE;
        @Comment("The blocks to hide or obfuscate")
        private List<Key> hiddenBlocks = List.of(
                Key.key("stone"), Key.key("deepslate"),
                Key.key("netherrack"), Key.key("end_stone"),
                Key.key("diamond_ore"), Key.key("deepslate_diamond_ore"),
                Key.key("iron_ore"), Key.key("deepslate_iron_ore"),
                Key.key("coal_ore"), Key.key("deepslate_coal_ore"),
                Key.key("emerald_ore"), Key.key("deepslate_emerald_ore"),
                Key.key("copper_ore"), Key.key("deepslate_copper_ore"),
                Key.key("redstone_ore"), Key.key("deepslate_redstone_ore"),
                Key.key("gold_ore"), Key.key("deepslate_gold_ore"),
                Key.key("lapis_ore"), Key.key("deepslate_lapis_ore"),
                Key.key("nether_gold_ore"), Key.key("nether_quartz_ore"),
                Key.key("ancient_debris"), Key.key("raw_copper_block"),
                Key.key("raw_iron_block")
        );

        public boolean isEnabled() {
            return this.enabled;
        }

        public EngineMode getEngineMode() {
            return this.engineMode;
        }

        public List<Key> getHiddenBlocks() {
            return this.hiddenBlocks;
        }

        public enum EngineMode {

            HIDE(ReplacementStrategy::replaceStaticZero),
            OBFUSCATE(ReplacementStrategy::replaceRandom),
            OBFUSCATE_LAYER(ReplacementStrategy::replaceRandomLayered),
            ;

            private final ReplacementStrategy.Ctor strategy;

            EngineMode(ReplacementStrategy.Ctor strategy) {
                this.strategy = strategy;
            }

            public ReplacementStrategy.Ctor getStrategy() {
                return this.strategy;
            }
        }
    }

    @ConfigSerializable
    public static final class BatchConfig {

        @Comment("Whether chunk batches are enabled or disabled in this dimension")
        private boolean enabled = true;
        @Comment("The amount of chunks in a single batch")
        private int chunksPerBatch = 12;
        @Comment("Whether BetterView should continue preparing chunks while waiting for acknowledgement of previous batch")
        private boolean prepareChunksWhileWaiting = true;
        @Comment("The amount of time without a chunk being sent after which a batch expires")
        private Duration batchTimeout = Duration.ofSeconds(5L);
        @Comment("The timeout after which BetterView automatically marks the batch as done, even without a player response")
        private Duration batchWaitTimeout = Duration.ofSeconds(30L);

        public boolean isEnabled() {
            return this.enabled;
        }

        public int getChunksPerBatch() {
            return this.chunksPerBatch;
        }

        public boolean isPrepareChunksWhileWaiting() {
            return !this.enabled || this.prepareChunksWhileWaiting;
        }

        public Duration getBatchTimeout() {
            return this.batchTimeout;
        }

        public Duration getBatchWaitTimeout() {
            return this.batchWaitTimeout;
        }
    }
}
