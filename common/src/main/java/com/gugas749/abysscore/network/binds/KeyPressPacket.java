package com.gugas749.abysscore.network.binds;

import com.gugas749.abysscore.api.network.AbyssPacketCodec;

/**
 * Client → Server: a keybind slot was pressed.
 * slot: 1-9
 */
public record KeyPressPacket(int slot) {

    public static final AbyssPacketCodec<KeyPressPacket> CODEC = AbyssPacketCodec.of(
            (buf, pkt) -> buf.writeInt(pkt.slot()),
            buf -> new KeyPressPacket(buf.readInt()));
}
