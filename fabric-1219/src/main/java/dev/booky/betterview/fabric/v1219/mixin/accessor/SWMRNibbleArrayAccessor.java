package dev.booky.betterview.fabric.v1219.mixin.accessor;
// Created by booky10 in BetterView (05:02 05.06.2025)

import ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@NullMarked
@Mixin(value = SWMRNibbleArray.class, remap = false)
public interface SWMRNibbleArrayAccessor {

    @Accessor(remap = false)
    byte[] getStorageVisible();
}
