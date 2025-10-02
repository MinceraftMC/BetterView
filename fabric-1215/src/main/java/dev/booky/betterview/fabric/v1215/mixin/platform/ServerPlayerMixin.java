package dev.booky.betterview.fabric.v1215.mixin.platform;
// Created by booky10 in BetterView (04:00 05.06.2025)

import ca.spottedleaf.moonrise.common.PlatformHooks;
import com.mojang.authlib.GameProfile;
import dev.booky.betterview.common.BetterViewPlayer;
import dev.booky.betterview.common.hooks.LevelHook;
import dev.booky.betterview.common.hooks.PlayerHook;
import dev.booky.betterview.common.util.BetterViewUtil;
import dev.booky.betterview.common.util.BypassedPacket;
import dev.booky.betterview.common.util.McChunkPos;
import dev.booky.betterview.fabric.v1215.packet.PacketUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@NullMarked
@Implements(@Interface(iface = PlayerHook.class, prefix = "betterview$"))
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {

    @Shadow
    public ServerGamePacketListenerImpl connection;

    @Unique
    private @MonotonicNonNull BetterViewPlayer bvPlayer;

    public ServerPlayerMixin(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile); // dummy ctor
    }

    @Shadow
    public abstract ServerLevel serverLevel();

    @Shadow
    public abstract int requestedViewDistance();

    @Shadow
    public abstract boolean hasDisconnected();

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void postInit(CallbackInfo ci) {
        this.bvPlayer = new BetterViewPlayer((PlayerHook) this);
    }

    @Inject(
            method = "restoreFrom",
            at = @At("TAIL")
    )
    private void onDeathRestore(ServerPlayer from, boolean keepInventory, CallbackInfo ci) {
        this.bvPlayer = ((PlayerHook) from).getBvPlayer();
    }

    public LevelHook betterview$getLevel() {
        return (LevelHook) this.serverLevel();
    }

    public McChunkPos betterview$getChunkPos() {
        ChunkPos pos = this.chunkPosition();
        return new McChunkPos(pos.x, pos.z);
    }

    public int betterview$getSendViewDistance() {
        return PlatformHooks.get().getSendViewDistance((ServerPlayer) (Object) this);
    }

    public int betterview$getRequestedViewDistance() {
        return this.requestedViewDistance();
    }

    public void betterview$sendViewDistancePacket(int distance) {
        ClientboundSetChunkCacheRadiusPacket packet = new ClientboundSetChunkCacheRadiusPacket(distance);
        ((PlayerHook) this).getNettyChannel().write(new BypassedPacket(packet));
    }

    public void betterview$sendChunkUnload(int chunkX, int chunkZ) {
        ByteBuf packetId = PacketUtil.FORGET_LEVEL_CHUNK_PACKET_ID_BUF.retainedSlice();
        ByteBuf chunkPos = BetterViewUtil.encodeChunkPos(McChunkPos.getChunkKey(chunkX, chunkZ));
        CompositeByteBuf packetBuf = BetterViewUtil.ALLOC.compositeBuffer(2)
                .addComponent(true, packetId)
                .addComponent(true, chunkPos);
        ((PlayerHook) this).getNettyChannel().write(new BypassedPacket(packetBuf));
    }

    public Channel betterview$getNettyChannel() {
        return this.connection.connection.channel;
    }

    public BetterViewPlayer betterview$getBvPlayer() {
        return this.bvPlayer;
    }

    public boolean betterview$isValid() {
        return !this.hasDisconnected() && this.isAlive();
    }
}
