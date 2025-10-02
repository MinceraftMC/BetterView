package dev.booky.betterview.nms.v1219;
// Created by booky10 in BetterView (21:19 03.06.2025)

import ca.spottedleaf.moonrise.common.util.WorldUtil;
import ca.spottedleaf.moonrise.patches.starlight.util.SaveUtil;
import com.mojang.serialization.Codec;
import dev.booky.betterview.common.antixray.AntiXrayProcessor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

import static dev.booky.betterview.nms.v1219.ChunkWriter.SENDABLE_HEIGHTMAP_TYPES;

@NullMarked
public final class ChunkTagTransformer {

    private static final long[][] EMPTY_LONG_2D_ARRAY = new long[0][];

    private ChunkTagTransformer() {
    }

    public static boolean isChunkLit(CompoundTag tag) {
        Optional<String> statusName = tag.getString("Status");
        if (statusName.isEmpty()) {
            return false; // missing data
        }
        ChunkStatus status = ChunkStatus.byName(statusName.get());
        if (!status.isOrAfter(ChunkStatus.LIGHT)) {
            return false; // not lit yet
        } else if (tag.get(SerializableChunkData.IS_LIGHT_ON_TAG) == null) {
            return false; // light isn't activated
        }
        // check whether starlight version matches
        Optional<Integer> lightVersion = tag.getInt(SaveUtil.STARLIGHT_VERSION_TAG);
        return lightVersion.isPresent() && lightVersion.get() == SaveUtil.getLightVersion();
    }

    private static boolean extractChunkData(
            ServerLevel level, CompoundTag chunkTag, ChunkPos pos,
            LevelChunkSection[] sections,
            byte[][] blockLight,
            byte @Nullable [][] skyLight
    ) {
        PalettedContainerFactory factory = level.palettedContainerFactory();
        Codec<PalettedContainer<Holder<Biome>>> biomeCodec = factory.biomeContainerRWCodec();
        Codec<PalettedContainer<BlockState>> blockCodec = factory.blockStatesContainerCodec();

        ListTag sectionTags = chunkTag.getListOrEmpty(SerializableChunkData.SECTIONS_TAG);
        int minLightSection = WorldUtil.getMinLightSection(level);

        boolean onlyAir = true;
        for (int i = 0; i < sectionTags.size(); ++i) {
            CompoundTag sectionTag = sectionTags.getCompound(i).orElseThrow();
            byte sectionY = sectionTag.getByte("Y").orElseThrow();
            int sectionIndex = level.getSectionIndexFromSectionY(sectionY);

            if (sectionIndex >= 0 && sectionIndex < sections.length) {
                PalettedContainer<BlockState> blocks = sectionTag.get("block_states") instanceof CompoundTag blockStatesTag
                        ? blockCodec.parse(NbtOps.INSTANCE, blockStatesTag).getOrThrow()
                        : factory.createForBlockStates();

                PalettedContainer<Holder<Biome>> biomes = sectionTag.get("biomes") instanceof CompoundTag biomesTag
                        ? biomeCodec.parse(NbtOps.INSTANCE, biomesTag).getOrThrow()
                        : factory.createForBiomes();

                LevelChunkSection section = new LevelChunkSection(blocks, biomes);
                sections[sectionIndex] = section;

                if (!section.hasOnlyAir()) {
                    onlyAir = false;
                }
            }

            if (sectionTag.get(SerializableChunkData.BLOCK_LIGHT_TAG) instanceof ByteArrayTag lightTag) {
                blockLight[sectionY - minLightSection] = lightTag.getAsByteArray();
            }
            if (skyLight != null && sectionTag.get(SerializableChunkData.SKY_LIGHT_TAG) instanceof ByteArrayTag lightTag) {
                skyLight[sectionY - minLightSection] = lightTag.getAsByteArray();
            }
        }
        return onlyAir;
    }

    private static long[] @Nullable [] extractHeightmapsData(CompoundTag chunkTag) {
        CompoundTag heightmaps = chunkTag.getCompoundOrEmpty(SerializableChunkData.HEIGHTMAPS_TAG);
        if (heightmaps.isEmpty()) {
            return EMPTY_LONG_2D_ARRAY;
        }
        long[] @Nullable [] heightmapsData = new long[SENDABLE_HEIGHTMAP_TYPES.length][];
        for (int i = 0, len = SENDABLE_HEIGHTMAP_TYPES.length; i < len; i++) {
            String key = SENDABLE_HEIGHTMAP_TYPES[i].getSerializationKey();
            if (heightmaps.get(key) instanceof LongArrayTag tag) {
                heightmapsData[i] = tag.getAsLongArray();
            }
        }
        return heightmapsData;
    }

    public static ByteBuf transformToBytesOrEmpty(
            ServerLevel level, CompoundTag chunkTag,
            @Nullable AntiXrayProcessor antiXray, ChunkPos pos
    ) {
        // extract relevant chunk data
        LevelChunkSection[] sections = new LevelChunkSection[level.getSectionsCount()];
        byte[][] blockLight = new byte[WorldUtil.getTotalLightSections(level)][];
        byte[][] skyLight = level.dimensionType().hasSkyLight() ? new byte[blockLight.length][] : null;
        boolean onlyAir = extractChunkData(level, chunkTag, pos, sections, blockLight, skyLight);
        if (onlyAir) {
            // empty, skip writing useless packet
            return Unpooled.EMPTY_BUFFER;
        }
        long[] @Nullable [] heightmapsData = extractHeightmapsData(chunkTag);
        // delegate to chunk writing method
        return ChunkWriter.writeFull(
                pos.x, pos.z, antiXray, level.getMinSectionY(),
                heightmapsData, sections, blockLight, skyLight
        );
    }
}
