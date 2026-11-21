package dev.booky.betterview.fabric.v263.mixin.accessor;
// Created by booky10 in BetterView (05:02 05.06.2025)

import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// string target so this class can load without Moonrise on the classpath
@NullMarked
@Mixin(targets = "ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray", remap = false)
public interface SWMRNibbleArrayAccessor {

    @Accessor(remap = false)
    byte[] getStorageVisible();
}
