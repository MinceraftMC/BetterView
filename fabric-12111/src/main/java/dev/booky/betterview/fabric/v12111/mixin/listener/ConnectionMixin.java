package dev.booky.betterview.fabric.v12111.mixin.listener;
// Created by booky10 in BetterView (05:08 05.06.2025)

import dev.booky.betterview.fabric.v12111.packet.PacketHandler;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@NullMarked
@Mixin(Connection.class)
public class ConnectionMixin {

    @Shadow
    @Final
    private PacketFlow receiving;

    @Inject(
            method = "configurePacketHandler",
            at = @At("TAIL")
    )
    private void postPacketHandlerConfig(ChannelPipeline pipeline, CallbackInfo ci) {
        if (this.receiving != PacketFlow.SERVERBOUND) {
            return; // wrong side, we only care about the server-side of connections
        }
        pipeline.addBefore("packet_handler", PacketHandler.HANDLER_NAME, new PacketHandler());
    }
}
