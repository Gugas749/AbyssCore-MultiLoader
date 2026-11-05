package com.gugas749.abysscore.network.region;

import com.gugas749.abysscore.api.network.AbyssPacketCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record NoEntryPacket(
    double exitX,
    double exitY,
    double exitZ,
    float  exitYaw  // 180° from entry direction — player faces away from the region
) {


    public static final AbyssPacketCodec<NoEntryPacket> CODEC = AbyssPacketCodec.of(
        (buf, pkt) -> {
            buf.writeDouble(pkt.exitX());
            buf.writeDouble(pkt.exitY());
            buf.writeDouble(pkt.exitZ());
            buf.writeFloat(pkt.exitYaw());
        },
        buf -> new NoEntryPacket(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble(),
            buf.readFloat()
        )
    );

}
