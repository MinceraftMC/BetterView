package dev.booky.betterview.platform;
// Created by booky10 in BetterView (16:27 03.06.2025)

import dev.booky.betterview.common.BetterViewManager;
import dev.booky.betterview.common.BetterViewPlayer;
import dev.booky.betterview.common.hooks.LevelHook;
import dev.booky.betterview.common.hooks.PlayerHook;
import dev.booky.betterview.common.util.BetterViewUtil;
import dev.booky.betterview.common.util.BypassedPacket;
import dev.booky.betterview.common.util.McChunkPos;
import dev.booky.betterview.nms.PaperNmsInterface;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PaperPlayer implements PlayerHook {

    private final BetterViewManager manager;
    private final Player player;
    private final BetterViewPlayer bvPlayer;

    public PaperPlayer(BetterViewManager manager, Player player) {
        this.manager = manager;
        this.player = player;
        this.bvPlayer = new BetterViewPlayer(this);
        PaperNmsInterface.SERVICE.saveNetworkPlayer(this.getNettyChannel(), this.bvPlayer);
    }

    @Override
    public LevelHook getLevel() {
        // don't need to cache this, called once per tick
        return this.manager.getLevel(this.player.getWorld().key());
    }

    @Override
    public McChunkPos getChunkPos() {
        return PaperNmsInterface.SERVICE.getChunkPos(this.player);
    }

    @Override
    public int getSendViewDistance() {
        return this.player.getSendViewDistance();
    }

    @Override
    public int getRequestedViewDistance() {
        return PaperNmsInterface.SERVICE.getRequestedViewDistance(this.player);
    }

    @Override
    public void sendViewDistancePacket(int distance) {
        Object packet = PaperNmsInterface.SERVICE.constructClientboundSetChunkCacheRadiusPacket(distance);
        this.getNettyChannel().write(new BypassedPacket(packet));
    }

    @Override
    public void sendChunkUnload(int chunkX, int chunkZ) {
        ByteBuf packetId = PaperNmsInterface.SERVICE.getClientboundForgetLevelChunkPacketId();
        ByteBuf chunkPos = BetterViewUtil.encodeChunkPos(McChunkPos.getChunkKey(chunkX, chunkZ));
        CompositeByteBuf packetBuf = BetterViewUtil.ALLOC.compositeBuffer(2)
                .addComponent(true, packetId)
                .addComponent(true, chunkPos);
        this.getNettyChannel().write(new BypassedPacket(packetBuf));
    }

    @Override
    public Channel getNettyChannel() {
        return PaperNmsInterface.SERVICE.getNettyChannel(this.player);
    }

    @Override
    public BetterViewPlayer getBvPlayer() {
        return this.bvPlayer;
    }

    @Override
    public boolean isValid() {
        return this.player.isConnected() && this.player.isValid();
    }
}
