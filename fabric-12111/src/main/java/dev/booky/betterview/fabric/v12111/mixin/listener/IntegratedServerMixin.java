package dev.booky.betterview.fabric.v12111.mixin.listener;
// Created by booky10 in BetterView (18:03 06.06.2025)

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.booky.betterview.common.config.BvConfig;
import dev.booky.betterview.fabric.v12111.BetterViewMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.server.IntegratedServer;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@NullMarked
@Environment(EnvType.CLIENT)
@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {

    @ModifyExpressionValue(
            method = "tickServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
                    ordinal = 0
            )
    )
    private Object replaceRenderDistance(Object original) {
        BvConfig config = BetterViewMod.INSTANCE.getManager().getConfig();
        int serverRenderDistance = config.getIntegratedServerRenderDistance();
        return serverRenderDistance == -1 ? original : serverRenderDistance;
    }
}
