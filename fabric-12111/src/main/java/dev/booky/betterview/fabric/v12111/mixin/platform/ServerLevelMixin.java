package dev.booky.betterview.fabric.v12111.mixin.platform;
// Created by booky10 in BetterView (04:24 05.06.2025)

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.booky.betterview.common.BetterViewManager;
import dev.booky.betterview.common.ChunkCacheEntry;
import dev.booky.betterview.common.antixray.AntiXrayProcessor;
import dev.booky.betterview.common.antixray.ReplacementPresets;
import dev.booky.betterview.common.config.BvLevelConfig;
import dev.booky.betterview.common.hooks.LevelHook;
import dev.booky.betterview.common.util.BetterViewUtil;
import dev.booky.betterview.common.util.ChunkTagResult;
import dev.booky.betterview.common.util.McChunkPos;
import dev.booky.betterview.fabric.v12111.BetterViewMod;
import dev.booky.betterview.fabric.v12111.packet.ChunkTagTransformer;
import dev.booky.betterview.fabric.v12111.packet.ChunkWriter;
import dev.booky.betterview.fabric.v12111.packet.MoonriseUtil;
import dev.booky.betterview.fabric.v12111.packet.PacketUtil;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

@NullMarked
@Implements(@Interface(iface = LevelHook.class, prefix = "betterview$"))
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level implements WorldGenLevel {

    @Shadow
    @Final
    private ServerChunkCache chunkSource;

    @Unique
    private @MonotonicNonNull LoadingCache<McChunkPos, ChunkCacheEntry> cache;
    @Unique
    private boolean voidWorld;
    @Unique
    private @MonotonicNonNull ByteBuf emptyChunkData;
    @Unique
    private @MonotonicNonNull AtomicInteger generatedChunks;
    @Unique
    private @MonotonicNonNull AntiXrayProcessor antiXray;

    public ServerLevelMixin(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates); // dummy ctor
    }

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void postInit(CallbackInfo ci) {
        this.emptyChunkData = PacketUtil.buildEmptyChunkData((ServerLevel) (Object) this, null);
        this.cache = BetterViewUtil.buildCache((LevelHook) this);
        this.voidWorld = PacketUtil.checkVoidWorld((ServerLevel) (Object) this);
        this.generatedChunks = new AtomicInteger();
        this.antiXray = createAntiXray((ServerLevel) (Object) this, ((LevelHook) this).getConfig().getAntiXray());
    }

    @Unique
    private static @Nullable AntiXrayProcessor createAntiXray(ServerLevel level, BvLevelConfig.AntiXrayConfig config) {
        // create replacement presets based on level type
        Function<Block, Integer> stateId = block -> Block.BLOCK_STATE_REGISTRY.getId(block.defaultBlockState());
        ReplacementPresets levelPresets = switch (level.dimension().identifier().toString()) {
            case "minecraft:the_nether" -> ReplacementPresets.createStatic(stateId.apply(Blocks.NETHERRACK));
            case "minecraft:the_end" -> ReplacementPresets.createStatic(stateId.apply(Blocks.END_STONE));
            default -> ReplacementPresets.createStaticZeroSplit(
                    new int[]{stateId.apply(Blocks.STONE)},
                    new int[]{stateId.apply(Blocks.DEEPSLATE)});
        };
        Function<Key, Stream<Integer>> stateListFn = key -> {
            Identifier blockKey = Identifier.fromNamespaceAndPath(key.namespace(), key.value());
            return BuiltInRegistries.BLOCK.get(blockKey)
                    .orElseThrow().value().getStateDefinition().getPossibleStates()
                    .stream().map(Block.BLOCK_STATE_REGISTRY::getIdOrThrow);
        };
        // create processor based on config
        return AntiXrayProcessor.createProcessor(config, levelPresets, stateListFn, Block.BLOCK_STATE_REGISTRY.size());
    }

    public BetterViewManager betterview$getManager() {
        return BetterViewMod.INSTANCE.getManager();
    }

    public CompletableFuture<@Nullable ByteBuf> betterview$getCachedChunkBuf(McChunkPos chunkPos) {
        return MoonriseUtil.getLoadedChunk((ServerLevel) (Object) this, chunkPos).thenApplyAsync(chunk -> {
            if (chunk != null) {
                return ChunkWriter.writeFullOrEmpty((ServerLevel) (Object) this, chunk, this.antiXray);
            }
            return null;
        });
    }

    public CompletableFuture<@Nullable ChunkTagResult> betterview$readChunk(McChunkPos chunkPos) {
        ChunkPos vanillaPos = new ChunkPos(chunkPos.getX(), chunkPos.getZ());
        return this.chunkSource.chunkMap.read(vanillaPos).thenApplyAsync(tag -> {
            if (tag.isEmpty()) {
                return null;
            } else if (!ChunkTagTransformer.isChunkLit(tag.get())) {
                return ChunkTagResult.EMPTY;
            }
            ByteBuf chunkBuf = ChunkTagTransformer.transformToBytesOrEmpty((ServerLevel) (Object) this,
                    tag.get(), this.antiXray, vanillaPos);
            return new ChunkTagResult(chunkBuf);
        });
    }

    public CompletableFuture<ByteBuf> betterview$loadChunk(int chunkX, int chunkZ) {
        return MoonriseUtil.getGeneratedChunk((ServerLevel) (Object) this, chunkX, chunkZ)
                .thenApplyAsync(chunk -> ChunkWriter.writeFullOrEmpty((ServerLevel) (Object) this, chunk, this.antiXray));
    }

    public boolean betterview$checkChunkGeneration() {
        if (BetterViewMod.INSTANCE.getManager().checkChunkGeneration()) {
            return this.generatedChunks.getAndIncrement() <= ((LevelHook) this).getConfig().getChunkGenerationLimit();
        }
        return false;
    }

    public void betterview$resetChunkGeneration() {
        this.generatedChunks.set(0);
    }

    public ByteBuf betterview$getEmptyChunkBuf(McChunkPos chunkPos) {
        ByteBuf posBuf = BetterViewUtil.encodeChunkPos(chunkPos.getX(), chunkPos.getZ());
        return BetterViewUtil.ALLOC.compositeBuffer(3)
                .addComponent(true, PacketUtil.LEVEL_CHUNK_WITH_LIGHT_PACKET_ID_BUF.retainedSlice())
                .addComponent(true, posBuf)
                .addComponent(true, this.emptyChunkData.retainedSlice());
    }

    public boolean betterview$isVoidWorld() {
        return this.voidWorld;
    }

    public Object betterview$dimension() {
        return this.dimension();
    }

    public BvLevelConfig betterview$getConfig() {
        return BetterViewMod.INSTANCE.getManager().getConfig(this.dimension().identifier());
    }

    public LoadingCache<McChunkPos, ChunkCacheEntry> betterview$getChunkCache() {
        return this.cache;
    }

    public Key betterview$getName() {
        return this.dimension().identifier();
    }
}
