package dev.booky.betterview.fabric.v1213.mixin.listener;
// Created by booky10 in BetterView (11:29 AM 13.04.2026)

import dev.booky.betterview.fabric.v1213.packet.MoonriseUtil;
import net.minecraft.server.level.ServerChunkCache;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@NullMarked
@Mixin(ServerChunkCache.class)
public class ServerChunkCacheMixin {

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerChunkCache;runDistanceManagerUpdates()Z",
                    shift = At.Shift.BEFORE
            )
    )
    private void preDistanceManagerTick(CallbackInfo ci) {
        MoonriseUtil.tickChunkGenerationQueue((ServerChunkCache) (Object) this);
    }
}
