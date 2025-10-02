package dev.booky.betterview.platform;
// Created by booky10 in BetterView (16:27 03.06.2025)

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.booky.betterview.common.BetterViewManager;
import dev.booky.betterview.common.ChunkCacheEntry;
import dev.booky.betterview.common.antixray.AntiXrayProcessor;
import dev.booky.betterview.common.config.BvLevelConfig;
import dev.booky.betterview.common.hooks.LevelHook;
import dev.booky.betterview.common.util.BetterViewUtil;
import dev.booky.betterview.common.util.ChunkTagResult;
import dev.booky.betterview.common.util.McChunkPos;
import dev.booky.betterview.nms.PaperNmsInterface;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@NullMarked
public class PaperLevel implements LevelHook {

    private final BetterViewManager manager;
    private final World world;

    private final ByteBuf emptyChunkData;
    private final LoadingCache<McChunkPos, ChunkCacheEntry> cache;
    private final boolean voidWorld;
    private final AtomicInteger generatedChunks = new AtomicInteger(0);
    private final @Nullable AntiXrayProcessor antiXray;

    public PaperLevel(BetterViewManager manager, World world) {
        this.manager = manager;
        this.world = world;
        this.emptyChunkData = PaperNmsInterface.SERVICE.buildEmptyChunkData(world, null);
        this.cache = BetterViewUtil.buildCache(this);
        this.voidWorld = PaperNmsInterface.SERVICE.checkVoidWorld(world);
        this.antiXray = PaperNmsInterface.SERVICE.createAntiXray(world, this.getConfig().getAntiXray());
    }

    @Override
    public CompletableFuture<@Nullable ByteBuf> getCachedChunkBuf(McChunkPos chunkPos) {
        return PaperNmsInterface.SERVICE.getLoadedChunkBuf(this.world, this.antiXray, chunkPos);
    }

    @Override
    public CompletableFuture<@Nullable ChunkTagResult> readChunk(McChunkPos chunkPos) {
        return PaperNmsInterface.SERVICE.readChunkTag(this.world, this.antiXray, chunkPos);
    }

    @Override
    public CompletableFuture<ByteBuf> loadChunk(int chunkX, int chunkZ) {
        return PaperNmsInterface.SERVICE.loadChunk(this.world, this.antiXray, chunkX, chunkZ);
    }

    @Override
    public boolean checkChunkGeneration() {
        if (this.manager.checkChunkGeneration()) {
            return this.generatedChunks.getAndIncrement() <= this.getConfig().getChunkGenerationLimit();
        }
        return false;
    }

    @Override
    public void resetChunkGeneration() {
        this.generatedChunks.set(0);
    }

    @Override
    public ByteBuf getEmptyChunkBuf(McChunkPos chunkPos) {
        ByteBuf posBuf = BetterViewUtil.encodeChunkPos(chunkPos.getX(), chunkPos.getZ());
        return BetterViewUtil.ALLOC.compositeBuffer(3)
                .addComponent(true, PaperNmsInterface.SERVICE.getClientboundLevelChunkWithLightPacketId())
                .addComponent(true, posBuf)
                .addComponent(true, this.emptyChunkData.retainedSlice());
    }

    @Override
    public boolean isVoidWorld() {
        return this.voidWorld;
    }

    @Override
    public Object dimension() {
        return PaperNmsInterface.SERVICE.getDimensionId(this.world);
    }

    @Override
    public BvLevelConfig getConfig() {
        return this.manager.getConfig(this.world.key());
    }

    @Override
    public LoadingCache<McChunkPos, ChunkCacheEntry> getChunkCache() {
        return this.cache;
    }

    @Override
    public Key getName() {
        return this.world.key();
    }
}
