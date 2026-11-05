package com.gugas749.abysscore.network.region;

import com.gugas749.abysscore.api.network.AbyssPacketCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

public record SubmitRegionUpdatePacket(
    String regionName,
    boolean delete,
    Set<String> tags,
    String entryFilterTag   // empty = no filter
) {


    public static final AbyssPacketCodec<SubmitRegionUpdatePacket> CODEC = AbyssPacketCodec.of(
        (buf, pkt) -> {
            buf.writeUtf(pkt.regionName());
            buf.writeBoolean(pkt.delete());
            buf.writeInt(pkt.tags().size());
            pkt.tags().forEach(buf::writeUtf);
            buf.writeUtf(pkt.entryFilterTag() != null ? pkt.entryFilterTag() : "");
        },
        buf -> {
            String name = buf.readUtf();
            boolean delete = buf.readBoolean();
            int tagCount = buf.readInt();
            Set<String> tags = new LinkedHashSet<>();
            for (int i = 0; i < tagCount; i++) tags.add(buf.readUtf());
            String filterTag = buf.readUtf();
            return new SubmitRegionUpdatePacket(name, delete, tags, filterTag);
        }
    );

}
