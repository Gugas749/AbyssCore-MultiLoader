package com.gugas749.abysscore.network.menu.packets;

import com.gugas749.abysscore.api.network.AbyssPacketCodec;
import com.gugas749.abysscore.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SaveTitlePacket(
    String id,           // empty = create new
    String name,
    String titleText,
    String subtitleText,
    int fadeIn,
    int stay,
    int fadeOut
) {


    public static final AbyssPacketCodec<SaveTitlePacket> CODEC = AbyssPacketCodec.of(
            (buf, pkt) -> {
                buf.writeUtf(pkt.id());
                buf.writeUtf(pkt.name());
                buf.writeUtf(pkt.titleText());
                buf.writeUtf(pkt.subtitleText());
                buf.writeInt(pkt.fadeIn());
                buf.writeInt(pkt.stay());
                buf.writeInt(pkt.fadeOut());
            },
            buf -> new SaveTitlePacket(
                    buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(),
                    buf.readInt(), buf.readInt(), buf.readInt()
            )
    );

}
