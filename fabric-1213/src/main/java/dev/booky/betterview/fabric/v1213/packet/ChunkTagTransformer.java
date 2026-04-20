package dev.booky.betterview.fabric.v1213.packet;
// Created by booky10 in BetterView (21:19 03.06.2025)

import ca.spottedleaf.moonrise.patches.starlight.util.SaveUtil;
import com.mojang.serialization.Codec;
import dev.booky.betterview.common.antixray.AntiXrayProcessor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import static dev.booky.betterview.fabric.v1213.packet.ChunkWriter.SENDABLE_HEIGHTMAP_TYPES;

@NullMarked
public final class ChunkTagTransformer {

    private ChunkTagTransformer() {
    }

    public static boolean isChunkLit(CompoundTag tag) {
        ChunkStatus status = ChunkStatus.byName(tag.getString("Status"));
        if (!status.isOrAfter(MoonriseUtil.TARGET_STATUS)) {
            return false; // not lit yet
        } else if (tag.get(SerializableChunkData.IS_LIGHT_ON_TAG) == null) {
            return false; // light isn't activated
        }
        if (!MoonriseUtil.INSTALLED) {
            return true; // vanilla light
        }
        // check whether starlight version matches
        int lightVersion = tag.getInt(SaveUtil.STARLIGHT_VERSION_TAG);
        return lightVersion == SaveUtil.getLightVersion();
    }

    private static boolean extractChunkData(
            ServerLevel level, CompoundTag chunkTag,
            LevelChunkSection[] sections,
            byte[][] blockLight,
            byte @Nullable [][] skyLight
    ) {
        Registry<Biome> biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        Codec<PalettedContainerRO<Holder<Biome>>> biomeCodec = SerializableChunkData.makeBiomeCodec(biomeRegistry);

        ListTag sectionTags = chunkTag.getList(SerializableChunkData.SECTIONS_TAG, Tag.TAG_COMPOUND);
        int minLightSection = level.getMinSectionY() - 1;

        boolean onlyAir = true;
        for (int i = 0; i < sectionTags.size(); ++i) {
            CompoundTag sectionTag = sectionTags.getCompound(i);
            byte sectionY = sectionTag.getByte("Y");
            int sectionIndex = level.getSectionIndexFromSectionY(sectionY);

            if (sectionIndex >= 0 && sectionIndex < sections.length) {
                PalettedContainer<BlockState> blocks;
                if (sectionTag.contains("block_states", Tag.TAG_COMPOUND)) {
                    blocks = SerializableChunkData.BLOCK_STATE_CODEC
                            .parse(NbtOps.INSTANCE, sectionTag.getCompound("block_states"))
                            .getOrThrow();
                } else {
                    blocks = new PalettedContainer<>(
                            Block.BLOCK_STATE_REGISTRY,
                            Blocks.AIR.defaultBlockState(),
                            PalettedContainer.Strategy.SECTION_STATES
                    );
                }

                PalettedContainerRO<Holder<Biome>> biomes;
                if (sectionTag.contains("biomes", Tag.TAG_COMPOUND)) {
                    biomes = biomeCodec
                            .parse(NbtOps.INSTANCE, sectionTag.getCompound("biomes"))
                            .getOrThrow();
                } else {
                    biomes = new PalettedContainer<>(
                            biomeRegistry.asHolderIdMap(),
                            biomeRegistry.getOrThrow(Biomes.PLAINS),
                            PalettedContainer.Strategy.SECTION_BIOMES
                    );
                }

                LevelChunkSection section = new LevelChunkSection(blocks, biomes);
                sections[sectionIndex] = section;

                if (!section.hasOnlyAir()) {
                    onlyAir = false;
                }
            }

            if (sectionTag.contains(SerializableChunkData.BLOCK_LIGHT_TAG, Tag.TAG_BYTE_ARRAY)) {
                blockLight[sectionY - minLightSection] = sectionTag.getByteArray(SerializableChunkData.BLOCK_LIGHT_TAG);
            }

            if (skyLight != null && sectionTag.contains(SerializableChunkData.SKY_LIGHT_TAG, Tag.TAG_BYTE_ARRAY)) {
                skyLight[sectionY - minLightSection] = sectionTag.getByteArray(SerializableChunkData.SKY_LIGHT_TAG);
            }
        }
        return onlyAir;
    }

    private static CompoundTag filterHeightmaps(CompoundTag chunkTag) {
        CompoundTag heightmaps = chunkTag.getCompound(SerializableChunkData.HEIGHTMAPS_TAG);
        if (heightmaps.isEmpty()) {
            return heightmaps;
        }
        CompoundTag filteredHeightmaps = new CompoundTag();
        for (int i = 0, len = SENDABLE_HEIGHTMAP_TYPES.length; i < len; i++) {
            String key = SENDABLE_HEIGHTMAP_TYPES[i].getSerializationKey();
            Tag heightmapsEntry = heightmaps.get(key);
            if (heightmapsEntry != null) {
                filteredHeightmaps.put(key, heightmapsEntry);
            }
        }
        return filteredHeightmaps;
    }

    public static ByteBuf transformToBytesOrEmpty(
            ServerLevel level, CompoundTag chunkTag,
            @Nullable AntiXrayProcessor antiXray, ChunkPos pos
    ) {
        // extract relevant chunk data
        LevelChunkSection[] sections = new LevelChunkSection[level.getSectionsCount()];
        byte[][] blockLight = new byte[MoonriseUtil.getTotalLightSections(level)][];
        byte[][] skyLight = level.dimensionType().hasSkyLight() ? new byte[blockLight.length][] : null;
        boolean onlyAir = extractChunkData(level, chunkTag, sections, blockLight, skyLight);
        if (onlyAir) {
            // empty, skip writing useless packet
            return Unpooled.EMPTY_BUFFER;
        }
        CompoundTag heightmapsTag = filterHeightmaps(chunkTag);
        // delegate to chunk writing method
        return ChunkWriter.writeFull(
                pos.x, pos.z, antiXray, level.getMinSectionY(),
                heightmapsTag, sections, blockLight, skyLight
        );
    }
}
