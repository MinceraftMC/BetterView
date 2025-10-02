package dev.booky.betterview.nms.v1211;
// Created by booky10 in BetterView (23:37 03.06.2025)

import dev.booky.betterview.common.BetterViewManager;
import dev.booky.betterview.common.hooks.PlayerHook;
import dev.booky.betterview.nms.ReflectionUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.TickRateManager;
import org.jspecify.annotations.NullMarked;

import java.lang.invoke.VarHandle;

// wrap the tick rate manager to inject into after moonrise has finished
// loading player chunk data and has sent view distance packets
@NullMarked
public class WrappedServerTickManager extends ServerTickRateManager {

    private static final VarHandle MINECRAFT_SERVER_TICK_RATE_MANAGER =
            ReflectionUtil.getField(MinecraftServer.class, ServerTickRateManager.class, 0);

    private static final VarHandle REMAINING_SPRINT_TICKS =
            ReflectionUtil.getField(ServerTickRateManager.class, long.class, 0);
    private static final VarHandle SPRINT_TICK_START_TIME =
            ReflectionUtil.getField(ServerTickRateManager.class, long.class, 1);
    private static final VarHandle SPRINT_TIME_SPEND =
            ReflectionUtil.getField(ServerTickRateManager.class, long.class, 2);
    private static final VarHandle SCHEDULED_CURRENT_SPRINT_TICKS =
            ReflectionUtil.getField(ServerTickRateManager.class, long.class, 3);
    private static final VarHandle PREVIOUS_IS_FROZEN =
            ReflectionUtil.getField(ServerTickRateManager.class, boolean.class, 0);
    private static final VarHandle SILENT =
            ReflectionUtil.getField(ServerTickRateManager.class, boolean.class, 1);

    private static final VarHandle TICKRATE =
            ReflectionUtil.getField(TickRateManager.class, float.class, 1);
    private static final VarHandle NANOSECONDS_PER_TICK =
            ReflectionUtil.getField(TickRateManager.class, long.class, 0);
    private static final VarHandle FROZEN_TICKS_TO_RUN =
            ReflectionUtil.getField(TickRateManager.class, int.class, 0);
    private static final VarHandle RUN_GAME_ELEMENTS =
            ReflectionUtil.getField(TickRateManager.class, boolean.class, 0);
    private static final VarHandle IS_FROZEN =
            ReflectionUtil.getField(TickRateManager.class, boolean.class, 1);

    private final BetterViewManager manager;
    private final ServerTickRateManager delegate;

    public WrappedServerTickManager(MinecraftServer server, BetterViewManager manager, ServerTickRateManager delegate) {
        super(server);
        this.manager = manager;
        this.delegate = delegate;

        // re-apply fields
        REMAINING_SPRINT_TICKS.set(this, REMAINING_SPRINT_TICKS.get(delegate));
        SPRINT_TICK_START_TIME.set(this, SPRINT_TICK_START_TIME.get(delegate));
        SPRINT_TIME_SPEND.set(this, SPRINT_TIME_SPEND.get(delegate));
        SCHEDULED_CURRENT_SPRINT_TICKS.set(this, SCHEDULED_CURRENT_SPRINT_TICKS.get(delegate));
        PREVIOUS_IS_FROZEN.set(this, PREVIOUS_IS_FROZEN.get(delegate));
        SILENT.set(this, SILENT.get(delegate));

        TICKRATE.set(this, TICKRATE.get(delegate));
        NANOSECONDS_PER_TICK.set(this, NANOSECONDS_PER_TICK.get(delegate));
        FROZEN_TICKS_TO_RUN.set(this, FROZEN_TICKS_TO_RUN.get(delegate));
        RUN_GAME_ELEMENTS.set(this, RUN_GAME_ELEMENTS.get(delegate));
        IS_FROZEN.set(this, IS_FROZEN.get(delegate));
    }

    public static void inject(MinecraftServer server, BetterViewManager manager) {
        ServerTickRateManager delegate = server.tickRateManager();
        WrappedServerTickManager wrapped = new WrappedServerTickManager(server, manager, delegate);
        MINECRAFT_SERVER_TICK_RATE_MANAGER.set(server, wrapped);
    }

    public static void uninject(MinecraftServer server) {
        if (server.tickRateManager() instanceof WrappedServerTickManager wrapped) {
            MINECRAFT_SERVER_TICK_RATE_MANAGER.set(server, wrapped.delegate);
        }
    }

    @Override
    public void updateJoiningPlayer(ServerPlayer player) {
        // trigger logic start
        PlayerHook hook = this.manager.getPlayerOrNull(player.getUUID());
        if (hook != null) {
            hook.getBvPlayer().tryTriggerStart();
        }

        super.updateJoiningPlayer(player);
    }
}
