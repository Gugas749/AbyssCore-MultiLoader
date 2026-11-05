package com.gugas749.abysscore.network.binds;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.api.network.AbyssPacketContext;
import com.gugas749.abysscore.features.bind.BindManager;
import com.gugas749.abysscore.features.bulk.BulkCommandData;
import com.gugas749.abysscore.features.bulk.BulkCommandManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Handles KeyPressPacket on the SERVER side.
 * Looks up what the player has bound to that slot and executes it.
 */
public class KeyPressHandler {
    public static void handle(KeyPressPacket packet, AbyssPacketContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            int slot = packet.slot();
            if (slot < 1 || slot > 9) return;

            String binding = BindManager.getBinding(player.getUUID(), slot);
            if (binding == null) return; // slot not bound, do nothing silently

            CommandSourceStack source = player.createCommandSourceStack();

            // Check if the binding matches a bulk command name first
            Optional<BulkCommandData> bulk = BulkCommandManager.get(binding);

            if (bulk.isPresent()) {
                // It's a bulk command — check its permission level
                BulkCommandData data = bulk.get();
                if (!player.hasPermissions(data.permLevel)) {
                    player.sendSystemMessage(
                            Component.translatable("message.abysscore.bulk.no_permission")
                    );
                    return;
                }

                // Execute each sub-command
                for (String cmd : data.commands) {
                    try {
                        player.getServer().getCommands().getDispatcher().execute(cmd, source);
                    } catch (Exception e) {
                        Constants.LOG.warn(
                                "[AbyssCore] Bind slot {} bulk sub-command failed for {}: {}",
                                slot, player.getName().getString(), e.getMessage()
                        );
                    }
                }
            } else {
                // It's a raw command — execute directly
                try {
                    player.getServer().getCommands().getDispatcher().execute(binding, source);
                } catch (Exception e) {
                    Constants.LOG.warn(
                            "[AbyssCore] Bind slot {} raw command failed for {}: {}",
                            slot, player.getName().getString(), e.getMessage()
                    );
                    player.sendSystemMessage(
                            Component.translatable("message.abysscore.bind.exec_failed", binding)
                    );
                }
            }
        });
    }
}
