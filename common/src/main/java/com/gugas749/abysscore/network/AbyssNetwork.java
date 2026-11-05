package com.gugas749.abysscore.network;

import net.minecraft.server.level.ServerPlayer;

/**
 * Short static helpers for sending AbyssCore's own packets from common code.
 * They just forward to PacketHandler.CHANNEL (kept so the existing callers didn't change).
 *
 * Packets are plain records registered in PacketHandler (no CustomPacketPayload anymore,
 * so the same classes work on Forge 1.20.1 and NeoForge 1.21.1).
 */
public class AbyssNetwork {

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        PacketHandler.CHANNEL.sendToPlayer(player, packet);
    }

    public static void sendToAll(Object packet) {
        PacketHandler.CHANNEL.sendToAll(packet);
    }

    public static void sendToServer(Object packet) {
        PacketHandler.CHANNEL.sendToServer(packet);
    }
}
