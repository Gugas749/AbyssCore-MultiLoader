package com.gugas749.abysscore.network.dimen;

import com.gugas749.abysscore.api.network.AbyssPacketCodec;

public record SubmitDimenCreatePacket(
        String name,
        String displayName,
        String style,     // "NORMAL", "SUPERFLAT", or "VOID"
        long   seed       // only used when style is NORMAL
) {

    public static final AbyssPacketCodec<SubmitDimenCreatePacket> CODEC = AbyssPacketCodec.of(
            (buf, pkt) -> {
                buf.writeUtf(pkt.name());
                buf.writeUtf(pkt.displayName());
                buf.writeUtf(pkt.style());
                buf.writeVarLong(pkt.seed());
            },
            buf -> new SubmitDimenCreatePacket(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarLong()));
}
