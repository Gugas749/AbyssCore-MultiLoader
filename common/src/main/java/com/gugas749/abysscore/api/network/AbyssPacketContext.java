package com.gugas749.abysscore.api.network;

import net.minecraft.world.entity.player.Player;

/**
 * Loader-neutral packet handling context passed to every packet handler method.
 *
 * Replaces NeoForge's IPayloadContext so handler classes in common compile without
 * any NeoForge imports.
 *
 * Each loader provides a thin adapter (e.g. AbyssNeoForgePacketContext) that wraps its
 * own context type and passes it here.
 *
 * Why does player() return Player instead of ServerPlayer?
 * On a C2S (client->server) handler the player IS a ServerPlayer — cast it yourself.
 * On a S2C (server->client) handler it is the local player entity, which might not be
 * a ServerPlayer in a pure-client context.  Returning the base Player keeps the
 * interface compatible with Java 17 and avoids NeoForge-specific cast rules.
 */
public interface AbyssPacketContext {

    /**
     * Schedules work to run on the main game thread.
     * Always call this inside handlers — packet threads are NOT the main thread.
     */
    void enqueueWork(Runnable runnable);

    /**
     * The player associated with this packet (sender for C2S, local player for S2C).
     */
    Player player();
}
