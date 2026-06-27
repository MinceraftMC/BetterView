package dev.booky.betterview.nms.v261;
// Created by booky10 in BetterView (22:48 03.06.2025)

import dev.booky.betterview.common.BetterViewPlayer;
import dev.booky.betterview.common.util.BypassedPacket;
import dev.booky.betterview.common.util.McChunkPos;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PacketHandler extends ChannelDuplexHandler {

    private @Nullable BetterViewPlayer player;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (this.player == null || !this.handle(msg)) {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // handle specific packets
        if (this.player != null && this.handle(msg)) {
            return;
        }
        // unwrap packet
        if (msg instanceof BypassedPacket) {
            msg = ((BypassedPacket) msg).packet();
        }
        // forward packet
        super.write(ctx, msg, promise);
    }

    /**
     * @return whether packet should be canceled
     */
    private boolean handle(Object input) {
        assert this.player != null;
        return switch (input) {
            case ClientboundLevelChunkWithLightPacket packet -> {
                this.player.serverChunkAdd(packet.getX(), packet.getZ());
                yield false;
            }
            case ClientboundForgetLevelChunkPacket packet -> {
                // if the chunk is still in range, cancel the unload packet
                ChunkPos chunkPos = packet.pos();
                yield this.player.serverChunkRemove(chunkPos.x(), chunkPos.z());
            }
            case ClientboundLoginPacket __ -> {
                this.player.handleDimensionReset(null);
                yield false;
            }
            case ClientboundStartConfigurationPacket __ -> {
                this.player.handleDimensionReset(null);
                yield false;
            }
            case ClientboundRespawnPacket packet -> {
                ResourceKey<Level> dimension = packet.commonPlayerSpawnInfo().dimension();
                this.player.handleDimensionReset(dimension);
                yield false;
            }
            case ClientboundSetChunkCacheRadiusPacket __ -> this.player.enabled;
            case ClientboundSetChunkCacheCenterPacket packet -> {
                this.player.move(new McChunkPos(packet.getX(), packet.getZ()));
                yield false;
            }
            case ServerboundPongPacket packet -> this.player.handleBatchPong(packet.getId());
            default -> false;
        };
    }

    public void setPlayer(@Nullable BetterViewPlayer player) {
        this.player = player;
    }
}
