package dev.booky.betterview.fabric.v1215.mixin.listener;
// Created by booky10 in BetterView (04:52 05.06.2025)

import com.llamalad7.mixinextras.sugar.Local;
import dev.booky.betterview.common.BetterViewManager;
import dev.booky.betterview.common.hooks.PlayerHook;
import dev.booky.betterview.fabric.v1215.BetterViewMod;
import dev.booky.betterview.fabric.v1215.packet.PacketHandler;
import io.netty.channel.ChannelHandler;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@NullMarked
@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Inject(
            method = "placeNewPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                    shift = At.Shift.AFTER,
                    remap = false
            )
    )
    private void postPlayerAdd(CallbackInfo ci, @Local(argsOnly = true) ServerPlayer player) {
        BetterViewManager manager = BetterViewMod.INSTANCE.getManager();
        manager.getPlayerOrNull(player.getUUID()); // load player
    }

    @Inject(
            method = "placeNewPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/Connection;setupInboundProtocol(Lnet/minecraft/network/ProtocolInfo;Lnet/minecraft/network/PacketListener;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void postGameProtocolSetup(
            CallbackInfo ci,
            @Local(argsOnly = true) Connection connection,
            @Local(argsOnly = true) ServerPlayer player
    ) {
        ChannelHandler handler = connection.channel.pipeline().get(PacketHandler.HANDLER_NAME);
        ((PacketHandler) handler).setPlayer(((PlayerHook) player).getBvPlayer());
    }

    @Inject(
            method = "respawn",
            at = @At("RETURN")
    )
    private void postPlayerRespawn(CallbackInfoReturnable<ServerPlayer> ci) {
        ServerPlayer player = ci.getReturnValue();

        // unregister player with this uuid and register again to handle respawning
        BetterViewManager manager = BetterViewMod.INSTANCE.getManager();
        manager.unregisterPlayer(player.getUUID());
        manager.getPlayerOrNull(player.getUUID()); // load new player instance

        // replace player hook in BV player (which was transferred from the previous player)
        ((PlayerHook) player).getBvPlayer().replacePlayer((PlayerHook) player);
    }

    @Inject(
            method = "remove",
            at = @At("TAIL")
    )
    private void postPlayerRemoval(ServerPlayer player, CallbackInfo ci) {
        BetterViewMod.INSTANCE.getManager().unregisterPlayer(player.getUUID());
    }
}
