package com.gugas749.abysscore.api.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * A mod's network channel. Get one from {@link AbyssNetworking#createChannel}.
 * Packets are plain records registered in that channel's registration method.
 *
 * Replaces NeoForge's static PacketDistributor.sendToX(...) and Forge's SimpleChannel.send(...),
 * so the same common code sends packets on both loaders.
 */
public interface AbyssChannel {

    void sendToPlayer(ServerPlayer player, Object packet);

    void sendToAll(Object packet);

    /** Every player that can see (is tracking) {@code entity} — not the entity itself. */
    void sendToTracking(Entity entity, Object packet);

    /** Every player tracking {@code entity}, AND the entity itself if it's a player. */
    void sendToTrackingAndSelf(Entity entity, Object packet);

    /** Client side only. */
    void sendToServer(Object packet);
}
