package com.gugas749.abysscore.network.vanish;

import com.gugas749.abysscore.api.network.AbyssPacketCodec;

/**
 * Server → Client: are you vanished, and should the HUD warning be drawn
 * (false when you turned the HUD_WARNING option off).
 */
public record VanishStateSyncPacket(boolean vanished, boolean showHud) {

    // Write and read the fields in the SAME order
    public static final AbyssPacketCodec<VanishStateSyncPacket> CODEC = AbyssPacketCodec.of(
            (buf, pkt) -> {
                buf.writeBoolean(pkt.vanished());
                buf.writeBoolean(pkt.showHud());
            },
            buf -> new VanishStateSyncPacket(buf.readBoolean(), buf.readBoolean()));
}
