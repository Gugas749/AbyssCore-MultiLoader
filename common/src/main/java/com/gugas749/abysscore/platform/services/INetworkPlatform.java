package com.gugas749.abysscore.platform.services;

import com.gugas749.abysscore.api.network.AbyssChannel;
import com.gugas749.abysscore.api.network.AbyssPacketRegistrar;

import java.util.function.Consumer;

/**
 * Loader-specific networking, loaded with Services (META-INF/services in each loader).
 *   Forge 1.20.1    → ForgeNetworkPlatform    (SimpleChannel, packets numbered by order)
 *   NeoForge 1.21.1 → NeoForgeNetworkPlatform (CustomPacketPayload wrappers, packets named)
 *
 * Common code doesn't call this directly: use AbyssNetworking.createChannel(...).
 */
public interface INetworkPlatform {

    AbyssChannel createChannel(String modId, String channelName, String version, Consumer<AbyssPacketRegistrar> register);
}
