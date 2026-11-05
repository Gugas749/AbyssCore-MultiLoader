package com.gugas749.abysscore.network.bulk;

import com.gugas749.abysscore.api.network.AbyssPacketCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Client → Server: submits a new bulk command definition from the GUI screen.
 */
public record SubmitBulkCommandPacket(
    String name,
    int permLevel,
    List<String> commands
) {

    public static final AbyssPacketCodec<SubmitBulkCommandPacket> CODEC = AbyssPacketCodec.of(
            (buf, pkt) -> {
                buf.writeUtf(pkt.name());
                buf.writeInt(pkt.permLevel());
                buf.writeInt(pkt.commands().size());
                for (String command : pkt.commands()) buf.writeUtf(command);
            },
            buf -> {
                String name = buf.readUtf();
                int permLevel = buf.readInt();
                int count = buf.readInt();
                List<String> commands = new ArrayList<>(count);
                for (int i = 0; i < count; i++) commands.add(buf.readUtf());
                return new SubmitBulkCommandPacket(name, permLevel, commands);
            });
}
