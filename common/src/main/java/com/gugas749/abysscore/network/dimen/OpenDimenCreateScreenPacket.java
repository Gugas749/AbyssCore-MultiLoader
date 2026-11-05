package com.gugas749.abysscore.network.dimen;

import com.gugas749.abysscore.api.network.AbyssPacketCodec;
import net.minecraft.resources.ResourceLocation;

public record OpenDimenCreateScreenPacket() {


    public static final AbyssPacketCodec<OpenDimenCreateScreenPacket> CODEC =
        AbyssPacketCodec.unit(new OpenDimenCreateScreenPacket());

}
