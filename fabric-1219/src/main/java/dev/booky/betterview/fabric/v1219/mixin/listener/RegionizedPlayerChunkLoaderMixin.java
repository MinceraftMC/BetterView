package dev.booky.betterview.fabric.v1219.mixin.listener;
// Created by booky10 in BetterView (03:59 05.06.2025)

import ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader;
import dev.booky.betterview.common.hooks.PlayerHook;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@NullMarked
@Mixin(value = RegionizedPlayerChunkLoader.class, remap = false)
public class RegionizedPlayerChunkLoaderMixin {

    @Inject(
            method = "addPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lca/spottedleaf/moonrise/patches/chunk_system/player/RegionizedPlayerChunkLoader$PlayerChunkLoaderData;add()V",
                    shift = At.Shift.AFTER,
                    remap = false
            ),
            remap = false
    )
    private void postPlayerLoaderAdd(ServerPlayer player, CallbackInfo ci) {
        ((PlayerHook) player).getBvPlayer().tryTriggerStart();
    }
}
