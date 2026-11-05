package com.gugas749.abysscore.api.network;

import com.gugas749.abysscore.platform.Services;

import java.util.function.Consumer;

/**
 * Entry point of the network API, for AbyssCore and the mods that use it.
 *
 *   public static final AbyssChannel CHANNEL =
 *       AbyssNetworking.createChannel("abyssevents", "1", ModPackets::register);
 *
 * Create the channel while your mod is being constructed (a static field read from your
 * mod constructor is enough): NeoForge registers packets in an early mod-bus event and Forge
 * needs the channel before the registries freeze.
 *
 * Behind it is the INetworkPlatform service: SimpleChannel on Forge, payloads on NeoForge.
 */
public final class AbyssNetworking {

    private AbyssNetworking() {}

    /**
     * @param modId    your mod id (channel / packet ids use it)
     * @param version  protocol version; client and server must match. Bump it when packets change.
     * @param register declares every packet, see {@link AbyssPacketRegistrar}
     */
    public static AbyssChannel createChannel(String modId, String version, Consumer<AbyssPacketRegistrar> register) {
        return createChannel(modId, "main", version, register);
    }

    /**
     * Same, for a mod that needs MORE than one channel. Each one needs its own name
     * (Forge: the SimpleChannel is modid:channelName). NeoForge only uses the packet names,
     * so those must also be unique across all of the mod's channels.
     */
    public static AbyssChannel createChannel(String modId, String channelName, String version,
                                             Consumer<AbyssPacketRegistrar> register) {
        return Services.NETWORK.createChannel(modId, channelName, version, register);
    }
}
