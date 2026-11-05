package com.gugas749.abysscore.network.blind;

import com.gugas749.abysscore.api.network.AbyssPacketCodec;

/** Server → Client: enables or disables the blind screen overlay. */
public record BlindSyncPacket(boolean blinded) {

    public static final AbyssPacketCodec<BlindSyncPacket> CODEC = AbyssPacketCodec.of(
            (buf, pkt) -> buf.writeBoolean(pkt.blinded()),
            buf -> new BlindSyncPacket(buf.readBoolean()));
}
