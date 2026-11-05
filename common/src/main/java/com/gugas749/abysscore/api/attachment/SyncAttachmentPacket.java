package com.gugas749.abysscore.api.attachment;

import com.gugas749.abysscore.api.network.AbyssPacketCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Server → Client: new value of one AbyssSyncedAttachment (encoded by AttachmentNbt). */
public record SyncAttachmentPacket(ResourceLocation id, CompoundTag data) {

    public static final AbyssPacketCodec<SyncAttachmentPacket> CODEC = AbyssPacketCodec.of(
            (buf, pkt) -> {
                buf.writeResourceLocation(pkt.id());
                buf.writeNbt(pkt.data());
            },
            buf -> {
                ResourceLocation id = buf.readResourceLocation();
                CompoundTag data = buf.readNbt();
                return new SyncAttachmentPacket(id, data != null ? data : new CompoundTag());
            });
}
