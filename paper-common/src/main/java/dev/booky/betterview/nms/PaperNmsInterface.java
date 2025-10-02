package dev.booky.betterview.nms;
// Created by booky10 in BetterView (16:21 03.06.2025)

import dev.booky.betterview.common.BetterViewManager;
import dev.booky.betterview.common.BetterViewPlayer;
import dev.booky.betterview.common.antixray.AntiXrayProcessor;
import dev.booky.betterview.common.config.BvLevelConfig;
import dev.booky.betterview.common.util.ChunkTagResult;
import dev.booky.betterview.common.util.McChunkPos;
import dev.booky.betterview.common.util.ServicesUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

@NullMarked
public interface PaperNmsInterface {

    PaperNmsInterface SERVICE = ServicesUtil.loadService(PaperNmsInterface.class);

    String BETTERVIEW_HANDLER = "betterview_handler";

    long getNanosPerServerTick();

    int getRequestedViewDistance(Player player);

    McChunkPos getChunkPos(Player player);

    Channel getNettyChannel(Player player);

    boolean isInjected(Channel channel);

    boolean isFakeChannel(Channel channel);

    Object constructClientboundSetChunkCacheRadiusPacket(int distance);

    ByteBuf getClientboundForgetLevelChunkPacketId();

    ByteBuf getClientboundLevelChunkWithLightPacketId();

    CompletableFuture<@Nullable ByteBuf> getLoadedChunkBuf(World world, @Nullable AntiXrayProcessor antiXray, McChunkPos chunkPos);

    CompletableFuture<@Nullable ChunkTagResult> readChunkTag(World world, @Nullable AntiXrayProcessor antiXray, McChunkPos chunkPos);

    CompletableFuture<ByteBuf> loadChunk(World world, @Nullable AntiXrayProcessor antiXray, int chunkX, int chunkZ);

    boolean checkVoidWorld(World world);

    Object getDimensionId(World world);

    ByteBuf buildEmptyChunkData(World world, @Nullable AntiXrayProcessor antiXray);

    void injectPacketHandler(BetterViewManager manager, NamespacedKey listenerKey);

    void uninjectPacketHandler(NamespacedKey listenerKey);

    void saveNetworkPlayer(Channel channel, BetterViewPlayer bvPlayer);

    @Nullable AntiXrayProcessor createAntiXray(World world, BvLevelConfig.AntiXrayConfig config);
}
