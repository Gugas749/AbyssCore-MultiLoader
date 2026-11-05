package com.gugas749.abysscore.api.permission;

import com.gugas749.abysscore.network.AbyssNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SERVER: keeps every client's AbyssClientPermission up to date.
 *
 * Instead of hooking every place a level can change (grant, revoke, /op, /deop — the last two
 * fire no event at all), it simply re-checks every online player every 5 seconds and sends
 * a packet ONLY when their level actually changed. Cheap and impossible to forget a case.
 *
 * The NeoForge event wiring lives in NeoForgeEventListeners, which calls these plain methods.
 */
public final class AbyssPermissionSync {

    private static final int CHECK_INTERVAL_TICKS = 100;   // 5 seconds

    /** Last level we told each player. */
    private static final Map<UUID, AbyssPermissionLevel> SENT = new HashMap<>();
    private static int tickCounter = 0;

    private AbyssPermissionSync() {}

    /** The level a player effectively has (ADMIN also counts OPs, see AbyssPermissionHandler.has). */
    public static AbyssPermissionLevel levelOf(ServerPlayer player) {
        if (AbyssPermissionHandler.has(player, AbyssPermissionLevel.ADMIN)) return AbyssPermissionLevel.ADMIN;
        if (AbyssPermissionHandler.has(player, AbyssPermissionLevel.MODERATOR)) return AbyssPermissionLevel.MODERATOR;
        return AbyssPermissionLevel.PLAYER;
    }

    /** Sends the player's level if it differs from what we last sent them. */
    public static void sync(ServerPlayer player) {
        AbyssPermissionLevel level = levelOf(player);
        if (SENT.put(player.getUUID(), level) != level) {
            AbyssNetwork.sendToPlayer(player, new PermissionSyncPacket(level));
        }
    }

    /**
     * Called on player login — LOWEST priority so AbyssPermissionHandler auto-grants ADMIN first,
     * and we sync AFTER that has happened.
     */
    public static void onLogin(ServerPlayer player) {
        SENT.remove(player.getUUID());   // fresh client → always send
        sync(player);
    }

    public static void onLogout(Player player) {
        SENT.remove(player.getUUID());
    }

    /** Called every server tick; syncs all players every CHECK_INTERVAL_TICKS ticks. */
    public static void onServerTick(MinecraftServer server) {
        if (++tickCounter < CHECK_INTERVAL_TICKS) return;
        tickCounter = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sync(player);
        }
    }
}
