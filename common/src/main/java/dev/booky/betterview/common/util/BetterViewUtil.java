package dev.booky.betterview.common.util;
// Created by booky10 in BetterView (15:15 03.06.2025)

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Ticker;
import dev.booky.betterview.common.ChunkCacheEntry;
import dev.booky.betterview.common.hooks.LevelHook;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class BetterViewUtil {

    private static final boolean ALLOC_UNPOOLED = Boolean.getBoolean("betterview.alloc.unpooled");
    private static final boolean ALLOC_HEAP = Boolean.getBoolean("betterview.alloc.heap");
    public static final ByteBufAllocator ALLOC = ALLOC_UNPOOLED
            ? new UnpooledByteBufAllocator(!ALLOC_HEAP)
            : new PooledByteBufAllocator(!ALLOC_HEAP);

    public static final int MC_MAX_DIMENSION_SIZE = 30_000_000;
    public static final int MC_MAX_DIMENSION_SIZE_CHUNKS = MC_MAX_DIMENSION_SIZE << 4;

    private BetterViewUtil() {
    }

    public static LoadingCache<McChunkPos, ChunkCacheEntry> buildCache(LevelHook level) {
        return Caffeine.newBuilder()
                .expireAfterWrite(level.getConfig().getCacheDuration())
                .ticker(Ticker.systemTicker())
                .<McChunkPos, ChunkCacheEntry>removalListener((key, val, cause) -> {
                    if (val != null) {
                        val.release();
                    }
                })
                .build(pos -> new ChunkCacheEntry(level, pos));
    }

    public static boolean isWithinRange(int posX, int posZ, int viewDistance) {
        int absX = Math.abs(posX);
        int absZ = Math.abs(posZ);
        // check done by extra moonrise logic
        int squareDist = Math.max(absX, absZ);
        if (squareDist > viewDistance + 1) {
            return false; // outside square distance
        }
        // check done by vanilla server logic
        long distX = Math.max(0, absX - 2);
        long distZ = Math.max(0, absZ - 2);
        long distXZSqrt = distX * distX + distZ * distZ;
        int viewDistanceSqrt = viewDistance * viewDistance;
        return distXZSqrt < viewDistanceSqrt; // outside cylindrical distance
    }

    public static ByteBuf encodeChunkPos(long chunkKey) {
        // same as vanilla encoding a chunk position directly
        return Unpooled.wrappedBuffer(new byte[]{
                (byte) (chunkKey >> 56), (byte) (chunkKey >> 48), (byte) (chunkKey >> 40), (byte) (chunkKey >> 32),
                (byte) (chunkKey >> 24), (byte) (chunkKey >> 16), (byte) (chunkKey >> 8), (byte) chunkKey,
        });
    }

    public static ByteBuf encodeChunkPos(int chunkX, int chunkZ) {
        // same as vanilla encoding the chunk x/z positions as 4-byte ints
        return Unpooled.wrappedBuffer(new byte[]{
                (byte) (chunkX >> 24), (byte) (chunkX >> 16), (byte) (chunkX >> 8), (byte) chunkX,
                (byte) (chunkZ >> 24), (byte) (chunkZ >> 16), (byte) (chunkZ >> 8), (byte) chunkZ,
        });
    }
}
