package dev.booky.betterview.fabric.v1219.mixin.listener;
// Created by booky10 in BetterView (04:11 05.06.2025)

import dev.booky.betterview.fabric.v1219.BetterViewMod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jspecify.annotations.NullMarked;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.Set;

@NullMarked
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Final @Shadow
    protected LevelStorageSource.LevelStorageAccess storageSource;

    @Shadow
    public abstract Set<ResourceKey<Level>> levelKeys();

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void postInit(CallbackInfo ci) {
        Path worldDir = this.storageSource.getLevelDirectory().path();
        BetterViewMod.INSTANCE.triggerPreLoad((MinecraftServer) (Object) this, worldDir);
    }

    @Inject(
            method = "runServer",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/server/MinecraftServer;status:Lnet/minecraft/network/protocol/status/ServerStatus;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER
            )
    )
    private void postServerInit(CallbackInfo ci) {
        BetterViewMod.INSTANCE.triggerPostLoad(this.levelKeys());
    }

    @Inject(
            method = "tickChildren",
            at = @At("TAIL")
    )
    private void postServerTick(CallbackInfo ci) {
        BetterViewMod.INSTANCE.getManager().runTick();
    }

    @Inject(
            method = "stopServer",
            at = @At(
                    // inject after players have been disconnected
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;removeAll()V",
                    shift = At.Shift.AFTER
            )
    )
    private void onShutdown(CallbackInfo ci) {
        BetterViewMod.INSTANCE.triggerShutdown();
    }
}
