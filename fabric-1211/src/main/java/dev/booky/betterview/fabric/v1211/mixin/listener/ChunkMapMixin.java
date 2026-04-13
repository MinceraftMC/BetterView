package dev.booky.betterview.fabric.v1211.mixin.listener;
// Created by booky10 in BetterView (6:48 PM 10.04.2026)

import dev.booky.betterview.common.hooks.PlayerHook;
import dev.booky.betterview.fabric.v1211.packet.MoonriseUtil;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@NullMarked
@Mixin(ChunkMap.class)
public class ChunkMapMixin {

    @Inject(
            method = "updatePlayerStatus",
            at = @At("TAIL")
    )
    private void postPlayerChunkTrack(ServerPlayer player, boolean added, CallbackInfo ci) {
        if (added && !MoonriseUtil.INSTALLED) {
            ((PlayerHook) player).getBvPlayer().tryTriggerStart();
        }
    }
}
