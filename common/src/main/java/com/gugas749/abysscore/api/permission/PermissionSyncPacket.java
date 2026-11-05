package com.gugas749.abysscore.api.permission;

import com.gugas749.abysscore.api.network.AbyssPacketCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Server → Client: "your AbyssCore permission level is now X". */
public record PermissionSyncPacket(AbyssPermissionLevel level) {


    public static final AbyssPacketCodec<PermissionSyncPacket> CODEC = AbyssPacketCodec.of(
            (buf, pkt) -> buf.writeEnum(pkt.level()),
            buf -> new PermissionSyncPacket(buf.readEnum(AbyssPermissionLevel.class))
    );

}
