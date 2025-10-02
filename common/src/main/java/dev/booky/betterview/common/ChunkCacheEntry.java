package dev.booky.betterview.common;
// Created by booky10 in TJCServer (21:07 12.05.2025)

import dev.booky.betterview.common.hooks.LevelHook;
import dev.booky.betterview.common.util.McChunkPos;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

// wrap cache value to allow dynamic "invalidation", as the validity of this cache entry
// is only known after asynchronously building the chunk byte buffer
@NullMarked
public final class ChunkCacheEntry {

    private final LevelHook level;
    private final McChunkPos pos;

    private volatile @Nullable CompletableFuture<@Nullable ByteBuf> future;
    private volatile boolean released = false;

    public ChunkCacheEntry(LevelHook level, McChunkPos pos) {
        this.level = level;
        this.pos = pos;
    }

    private CompletableFuture<@Nullable ByteBuf> tryGet() {
        LevelHook level = this.level;
        McChunkPos pos = this.pos;

        // load chunk from level cache; I'm not sure whether this is thread-safe or not,
        // but this hasn't caused any issues yet - so I'll just assume it's thread-safe :)
        return level.getCachedChunkBuf(pos).thenCompose(chunkBuf -> {
            if (chunkBuf != null) { // chunk is already loaded, return buffer
                return CompletableFuture.completedFuture(chunkBuf);
            }
            // read chunk directly from storage; this is passed to an IO thread anyway, so it
            // doesn't matter that this is called off-thread I think
            return level.readChunk(pos).thenCompose(chunkTag -> {
                // if the is fully lit (chunk upgrading is done while reading), create chunk data directly
                if (chunkTag != null && chunkTag.buffer() != null) {
                    return CompletableFuture.completedFuture(chunkTag.buffer());
                } else if (chunkTag == null && level.isVoidWorld()) {
                    // skip useless chunk generation if this is a void world
                    return CompletableFuture.completedFuture(Unpooled.EMPTY_BUFFER);
                } else if (!level.checkChunkGeneration()) {
                    // we aren't allowed to generate new chunks, return null
                    return CompletableFuture.completedFuture(null);
                }
                // call moonrise chunk system to generate chunk to LIGHT stage
                return level.loadChunk(pos.getX(), pos.getZ());
            });
        });
    }

    public CompletableFuture<@Nullable ByteBuf> get() {
        // all of this code runs synchronized on this cache entry
        synchronized (this) {
            if (this.released) {
                throw new IllegalStateException("Can't get buffer for " + this.pos + " in "
                        + this.level.getName() + ", cache entry has already been released");
            }
            {
                CompletableFuture<@Nullable ByteBuf> future = this.future;
                if (future != null) {
                    // future is already set, return
                    return future;
                }
            }
            CompletableFuture<@Nullable ByteBuf> future = CompletableFuture.completedFuture(null)
                    // launch chunk handling asynchronously to minimize lock time
                    .thenComposeAsync(__ -> this.tryGet())
                    .thenApply(buf -> {
                        // if the bytebuf is null, clear the future; this makes it
                        // possible to retry getting this chunk on the next attempt
                        if (buf == null) {
                            // can't cause deadlock as this is run asynchronously
                            synchronized (this) {
                                this.future = null;
                            }
                        }
                        return buf;
                    });
            // save the future to return for later calls; this future will
            // be cleared if getting the chunk fails (see above)
            this.future = future;
            return future;
        }
    }

    public void release() {
        synchronized (this) {
            CompletableFuture<@Nullable ByteBuf> future = this.future;
            if (future != null) {
                future.thenAccept(ReferenceCountUtil::release);
            }
            // clear saved future and mark chunk as released
            this.future = null;
            this.released = true;
        }
    }
}
