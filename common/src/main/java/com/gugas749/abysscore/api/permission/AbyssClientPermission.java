package com.gugas749.abysscore.api.permission;

import com.gugas749.abysscore.api.network.AbyssPacketContext;

/**
 * CLIENT: this player's own permission level, as last told by the server.
 *
 *   if (AbyssClientPermission.has(AbyssPermissionLevel.MODERATOR)) { show staff button }
 *
 * For the USER EXPERIENCE only (hide buttons, refuse to open staff screens). A modified client
 * can fake this value, so the server must still check AbyssPermissionHandler on every action.
 * Event wiring (NeoForge) lives in NeoForgeClientEventListeners; only plain methods here.
 */
public final class AbyssClientPermission {

    private static AbyssPermissionLevel level = AbyssPermissionLevel.PLAYER;

    private AbyssClientPermission() {}

    public static AbyssPermissionLevel get() {
        return level;
    }

    public static boolean has(AbyssPermissionLevel required) {
        return level.isAtLeast(required);
    }

    public static void handle(PermissionSyncPacket packet, AbyssPacketContext ctx) {
        ctx.enqueueWork(() -> level = packet.level());
    }

    /** Called by NeoForgeClientEventListeners on player logout. Resets to PLAYER. */
    public static void onLoggingOut() {
        level = AbyssPermissionLevel.PLAYER;
    }
}
