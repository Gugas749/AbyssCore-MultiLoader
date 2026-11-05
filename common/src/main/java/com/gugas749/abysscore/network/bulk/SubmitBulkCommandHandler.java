package com.gugas749.abysscore.network.bulk;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.api.network.AbyssPacketContext;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import com.gugas749.abysscore.features.bulk.BulkCommandData;
import com.gugas749.abysscore.features.bulk.BulkCommandManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles SubmitBulkCommandPacket on the SERVER side.
 * Validates the sender is MODERATOR+ (same as /abysscore bulk), then saves the new bulk command.
 */
public class SubmitBulkCommandHandler {

    public static void handle(SubmitBulkCommandPacket packet, AbyssPacketContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            // Security: MODERATOR+ can create bulk commands
            if (!AbyssPermissionHandler.require(player, AbyssPermissionLevel.MODERATOR)) {
                Constants.LOG.warn(
                    "[AbyssCore] Player {} tried to create a bulk command without MODERATOR permissions!",
                    player.getName().getString()
                );
                return;
            }
            String name = packet.name().trim().toLowerCase();

            // Validate name
            if (name.isEmpty() || !name.matches("[a-z0-9_]+")) {
                player.sendSystemMessage(
                    Component.translatable("message.abysscore.bulk.invalid_name")
                );
                return;
            }

            // Validate commands list
            if (packet.commands().isEmpty()) {
                player.sendSystemMessage(
                    Component.translatable("message.abysscore.bulk.no_commands")
                );
                return;
            }

            // Save
            BulkCommandData data = new BulkCommandData(name, packet.permLevel(), packet.commands());
            boolean added = BulkCommandManager.add(data);

            if (!added) {
                player.sendSystemMessage(
                    Component.translatable("message.abysscore.bulk.already_exists", name)
                );
                return;
            }

            player.sendSystemMessage(
                Component.translatable("message.abysscore.bulk.created", name, packet.commands().size())
            );

            Constants.LOG.info(
                "[AbyssCore] {} created bulk command '{}' with {} sub-command(s).",
                player.getName().getString(), name, packet.commands().size()
            );
        });
    }
}
