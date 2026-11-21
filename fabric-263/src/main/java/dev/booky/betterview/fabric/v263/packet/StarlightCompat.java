package dev.booky.betterview.fabric.v263.packet;
// isolates Moonrise/Starlight types

import ca.spottedleaf.moonrise.patches.starlight.chunk.StarlightChunk;
import ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray;
import ca.spottedleaf.moonrise.patches.starlight.util.SaveUtil;
import dev.booky.betterview.fabric.v263.mixin.accessor.SWMRNibbleArrayAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

// Only loaded when Moonrise is present. Keeps Starlight types out of the vanilla classes.
@NullMarked
public final class StarlightCompat {
    public static boolean isMatchingStarlightVersion(CompoundTag tag) {
        Optional<Integer> lightVersion = tag.getInt(SaveUtil.STARLIGHT_VERSION_TAG);
        return lightVersion.isPresent() && lightVersion.get() == SaveUtil.getLightVersion();
    }

    public static byte @Nullable [][] blockLight(ChunkAccess chunk, boolean allowEmpty) {
        return convertStarlightToBytes(((StarlightChunk) chunk).starlight$getBlockNibbles(), allowEmpty);
    }

    public static byte @Nullable [][] skyLight(ChunkAccess chunk, boolean allowEmpty) {
        return convertStarlightToBytes(((StarlightChunk) chunk).starlight$getSkyNibbles(), allowEmpty);
    }

    private static byte @Nullable [][] convertStarlightToBytes(SWMRNibbleArray[] layers, boolean allowEmpty) {
        int layerCount = layers.length;
        byte[][] byteLayers = new byte[layerCount][];
        boolean converted = false;
        for (int i = 0; i < layerCount; i++) {
            SWMRNibbleArray layer = layers[i];
            if (layer.isInitialisedVisible()) {
                byteLayers[i] = ((SWMRNibbleArrayAccessor) (Object) layer).getStorageVisible();
                converted = true;
            }
        }
        return converted || !allowEmpty ? byteLayers : null;
    }
}
