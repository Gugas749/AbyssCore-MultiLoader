package com.gugas749.abysscore.api.attachment;

import com.gugas749.abysscore.api.network.AbyssPacketContext;

/** CLIENT: stores the value the server sent in the attachment's client copy. */
public final class SyncAttachmentClientHandler {

    private SyncAttachmentClientHandler() {}

    public static void handle(SyncAttachmentPacket packet, AbyssPacketContext ctx) {
        ctx.enqueueWork(() -> {
            AbyssSyncedAttachment<?> attachment = AbyssSyncedAttachment.byId(packet.id());
            if (attachment != null) attachment.applyClientValue(packet.data());
        });
    }
}
