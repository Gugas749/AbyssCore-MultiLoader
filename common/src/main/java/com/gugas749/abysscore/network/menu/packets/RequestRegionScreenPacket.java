package com.gugas749.abysscore.network.menu.packets;

import com.gugas749.abysscore.api.network.AbyssPacketCodec;
import com.gugas749.abysscore.Constants;
import net.minecraft.resources.ResourceLocation;

public record RequestRegionScreenPacket() {
    public static final AbyssPacketCodec<RequestRegionScreenPacket> CODEC =
            AbyssPacketCodec.unit(new RequestRegionScreenPacket());
}
