package dev.booky.betterview.nms.v1219;
// Created by booky10 in BetterView (16:37 03.06.2025)

import ca.spottedleaf.concurrentutil.util.Priority;
import ca.spottedleaf.moonrise.common.PlatformHooks;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import dev.booky.betterview.common.BetterViewManager;
import dev.booky.betterview.common.BetterViewPlayer;
import dev.booky.betterview.common.antixray.AntiXrayProcessor;
import dev.booky.betterview.common.antixray.ReplacementPresets;
import dev.booky.betterview.common.config.BvLevelConfig;
import dev.booky.betterview.common.util.ChunkTagResult;
import dev.booky.betterview.common.util.McChunkPos;
import dev.booky.betterview.nms.PaperNmsInterface;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import net.kyori.adventure.key.Key;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

@NullMarked
public class NmsAdapter implements PaperNmsInterface {

    // magic packet id values
    static final byte FORGET_LEVEL_CHUNK_PACKET_ID = 0x25;
    static final ByteBuf FORGET_LEVEL_CHUNK_PACKET_ID_BUF =
            Unpooled.wrappedBuffer(new byte[]{FORGET_LEVEL_CHUNK_PACKET_ID});
    static final byte LEVEL_CHUNK_WITH_LIGHT_PACKET_ID = 0x2C;
    static final ByteBuf LEVEL_CHUNK_WITH_LIGHT_PACKET_ID_BUF =
            Unpooled.wrappedBuffer(new byte[]{LEVEL_CHUNK_WITH_LIGHT_PACKET_ID});

    public NmsAdapter() {
        if (SharedConstants.getProtocolVersion() != 773) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public long getNanosPerServerTick() {
        return MinecraftServer.getServer().tickRateManager().nanosecondsPerTick();
    }

    @Override
    public int getRequestedViewDistance(Player player) {
        return ((CraftPlayer) player).getHandle().requestedViewDistance();
    }

    @Override
    public McChunkPos getChunkPos(Player player) {
        ChunkPos pos = ((CraftPlayer) player).getHandle().chunkPosition();
        return new McChunkPos(pos.x, pos.z);
    }

    @Override
    public Channel getNettyChannel(Player player) {
        return ((CraftPlayer) player).getHandle().connection.connection.channel;
    }

    @Override
    public Object constructClientboundSetChunkCacheRadiusPacket(int distance) {
        return new ClientboundSetChunkCacheRadiusPacket(distance);
    }

    @Override
    public ByteBuf getClientboundForgetLevelChunkPacketId() {
        return FORGET_LEVEL_CHUNK_PACKET_ID_BUF.retainedSlice();
    }

    @Override
    public ByteBuf getClientboundLevelChunkWithLightPacketId() {
        return LEVEL_CHUNK_WITH_LIGHT_PACKET_ID_BUF.retainedSlice();
    }

    @Override
    public CompletableFuture<@Nullable ByteBuf> getLoadedChunkBuf(World world, @Nullable AntiXrayProcessor antiXray, McChunkPos chunkPos) {
        ServerLevel level = ((CraftWorld) world).getHandle();
        NewChunkHolder holder = level.moonrise$getChunkTaskScheduler().chunkHolderManager.getChunkHolder(chunkPos.getKey());
        if (holder == null) {
            return CompletableFuture.completedFuture(null);
        }
        ChunkAccess access = holder.getChunkIfPresent(ChunkStatus.LIGHT);
        if (access == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> ChunkWriter.writeFullOrEmpty(access, antiXray));
    }

    @Override
    public CompletableFuture<@Nullable ChunkTagResult> readChunkTag(World world, @Nullable AntiXrayProcessor antiXray, McChunkPos chunkPos) {
        ServerLevel level = ((CraftWorld) world).getHandle();
        ChunkPos nmsPos = new ChunkPos(chunkPos.getX(), chunkPos.getZ());
        return level.chunkSource.chunkMap.read(nmsPos).thenApplyAsync(tag -> {
            if (tag.isEmpty()) {
                return null;
            } else if (!ChunkTagTransformer.isChunkLit(tag.get())) {
                return ChunkTagResult.EMPTY;
            }
            ByteBuf chunkBuf = ChunkTagTransformer.transformToBytesOrEmpty(level, tag.get(), antiXray, nmsPos);
            return new ChunkTagResult(chunkBuf);
        });
    }

    @Override
    public CompletableFuture<ByteBuf> loadChunk(World world, @Nullable AntiXrayProcessor antiXray, int chunkX, int chunkZ) {
        CompletableFuture<ByteBuf> future = new CompletableFuture<>();
        ServerLevel level = ((CraftWorld) world).getHandle();
        PlatformHooks.get().scheduleChunkLoad(level, chunkX, chunkZ, true, ChunkStatus.LIGHT, true, Priority.LOW,
                chunk -> future.completeAsync(() -> ChunkWriter.writeFullOrEmpty(chunk, antiXray)));
        return future;
    }

    @Override
    public boolean checkVoidWorld(World world) {
        ServerLevel level = ((CraftWorld) world).getHandle();
        if (level.chunkSource.getGenerator() instanceof FlatLevelSource flat) {
            return flat.settings().getLayers().stream()
                    .noneMatch(state -> state != null && !state.isAir());
        }
        return false;
    }

    @Override
    public Object getDimensionId(World world) {
        return ((CraftWorld) world).getHandle().dimension();
    }

    @Override
    public ByteBuf buildEmptyChunkData(World world, @Nullable AntiXrayProcessor antiXray) {
        ServerLevel level = ((CraftWorld) world).getHandle();
        Registry<Biome> biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        Holder.Reference<Biome> biome = biomeRegistry.getOrThrow(Biomes.THE_VOID);
        EmptyLevelChunk chunk = new EmptyLevelChunk(level, ChunkPos.ZERO, biome);

        ByteBuf buf = Unpooled.buffer();
        try {
            long[] @Nullable [] heightmapsData = ChunkWriter.extractHeightmapsData(chunk);
            byte[][] blockLight = LightWriter.convertStarlightToBytes(chunk.starlight$getBlockNibbles(), false);
            byte[][] skyLight = LightWriter.convertStarlightToBytes(chunk.starlight$getSkyNibbles(), true);
            ChunkWriter.writeFullBody(buf, antiXray, level.getMinSectionY(),
                    heightmapsData, chunk.getSections(), blockLight, skyLight);
            return buf.retain();
        } finally {
            buf.release();
        }
    }

    @Override
    public void injectPacketHandler(BetterViewManager manager, NamespacedKey listenerKey) {
        ChannelInitializeListenerHolder.addListener(listenerKey, channel -> channel.pipeline()
                .addBefore("packet_handler", BETTERVIEW_HANDLER, new PacketHandler()));
        // inject existing connections
        MinecraftServer server = MinecraftServer.getServer();
        for (Connection connection : server.getConnection().getConnections()) {
            connection.channel.pipeline()
                    .addBefore("packet_handler", BETTERVIEW_HANDLER, new PacketHandler());
        }
        // inject post-join handling
        WrappedServerTickManager.inject(server, manager);
    }

    @Override
    public void uninjectPacketHandler(NamespacedKey listenerKey) {
        ChannelInitializeListenerHolder.removeListener(listenerKey);
        // uninject existing connections
        MinecraftServer server = MinecraftServer.getServer();
        for (Connection connection : server.getConnection().getConnections()) {
            connection.channel.pipeline().remove(BETTERVIEW_HANDLER);
        }
        // uninject post-join handling
        WrappedServerTickManager.uninject(server);
    }

    @Override
    public void saveNetworkPlayer(Channel channel, BetterViewPlayer bvPlayer) {
        PacketHandler handler = (PacketHandler) channel.pipeline().get(BETTERVIEW_HANDLER);
        if (handler == null) {
            throw new IllegalStateException("Can't save network player to " + channel + ", no handler found");
        }
        handler.setPlayer(bvPlayer);
    }

    @Override
    public @Nullable AntiXrayProcessor createAntiXray(World world, BvLevelConfig.AntiXrayConfig config) {
        // create replacement presets based on level type
        Function<Block, Integer> stateId = block -> Block.BLOCK_STATE_REGISTRY.getId(block.defaultBlockState());
        ReplacementPresets levelPresets = switch (world.getEnvironment()) {
            case NETHER -> ReplacementPresets.createStatic(stateId.apply(Blocks.NETHERRACK));
            case THE_END -> ReplacementPresets.createStatic(stateId.apply(Blocks.END_STONE));
            default -> ReplacementPresets.createStaticZeroSplit(
                    new int[]{stateId.apply(Blocks.STONE)},
                    new int[]{stateId.apply(Blocks.DEEPSLATE)});
        };
        Function<Key, Stream<Integer>> stateListFn = key -> {
            ResourceLocation blockKey = ResourceLocation.fromNamespaceAndPath(key.namespace(), key.value());
            return BuiltInRegistries.BLOCK.get(blockKey)
                    .orElseThrow().value().getStateDefinition().getPossibleStates()
                    .stream().map(Block.BLOCK_STATE_REGISTRY::getIdOrThrow);
        };
        // create processor based on config
        return AntiXrayProcessor.createProcessor(config, levelPresets, stateListFn, Block.BLOCK_STATE_REGISTRY.size());
    }
}
