package dev.booky.betterview.nms.v1215;
// Created by booky10 in BetterView (22:48 03.06.2025)

import dev.booky.betterview.common.BetterViewPlayer;
import dev.booky.betterview.common.util.BypassedPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PacketHandler extends ChannelOutboundHandlerAdapter {

    private @Nullable BetterViewPlayer player;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // handle specific packets
        if (this.handle(msg)) {
            return;
        }
        // unwrap packet
        if (msg instanceof BypassedPacket) {
            msg = ((BypassedPacket) msg).packet();
        }
        // forward packet
        super.write(ctx, msg, promise);
    }

    private boolean handle(Object input) {
        if (input instanceof ClientboundLevelChunkWithLightPacket
                || input instanceof ClientboundForgetLevelChunkPacket
                || input instanceof ClientboundLoginPacket
                || input instanceof ClientboundStartConfigurationPacket
                || input instanceof ClientboundRespawnPacket
                || input instanceof ClientboundSetChunkCacheRadiusPacket) {
            if (this.player != null && this.player.enabled) {
                switch (input) {
                    case ClientboundLevelChunkWithLightPacket packet ->
                            this.player.serverChunkAdd(packet.getX(), packet.getZ());
                    case ClientboundForgetLevelChunkPacket packet -> {
                        // if the chunk is still in range, cancel the unload packet
                        ChunkPos chunkPos = packet.pos();
                        return this.player.serverChunkRemove(chunkPos.x, chunkPos.z);
                    }
                    case ClientboundLoginPacket __ -> this.player.handleDimensionReset(null);
                    case ClientboundStartConfigurationPacket __ -> this.player.handleDimensionReset(null);
                    case ClientboundRespawnPacket packet -> {
                        ResourceKey<Level> dimension = packet.commonPlayerSpawnInfo().dimension();
                        this.player.handleDimensionReset(dimension);
                    }
                    case ClientboundSetChunkCacheRadiusPacket __ -> {
                        return true; // always cancel if enabled
                    }
                    default -> {
                        // NO-OP
                    }
                }
            }
        }
        return false; // don't cancel packet
    }

    public void setPlayer(@Nullable BetterViewPlayer player) {
        this.player = player;
    }
}
