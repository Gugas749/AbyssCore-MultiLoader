package com.gugas749.abysscore.network.dimen;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.api.network.AbyssPacketContext;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import com.gugas749.abysscore.features.dimen.ACDimensionData;
import com.gugas749.abysscore.features.dimen.ACDimensionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Random;

public class DimenPacketHandlers {

    public static void handleCreate(SubmitDimenCreatePacket packet, AbyssPacketContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!AbyssPermissionHandler.require(player, AbyssPermissionLevel.ADMIN)) return;   // same as /abysscore dimen

            String name = packet.name().trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
            if (name.isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.abysscore.dimen.invalid_name"));
                return;
            }

            if (ACDimensionManager.exists(name)) {
                player.sendSystemMessage(
                        Component.translatable("message.abysscore.dimen.already_exists", name));
                return;
            }

            // Use a random seed if the user left it at 0
            long seed = packet.seed() == 0 ? new Random().nextLong() : packet.seed();

            String displayName = packet.displayName().isBlank() ? name : packet.displayName().trim();

            ACDimensionData data = new ACDimensionData(
                    name,
                    displayName,
                    ACDimensionData.Style.fromString(packet.style()),
                    seed
            );

            boolean success = ACDimensionManager.create(data, player.getServer());

            if (success) {
                player.sendSystemMessage(
                        Component.translatable("message.abysscore.dimen.created_pending", name));
                Constants.LOG.info("[AbyssCore] {} created dimension '{}'.",
                        player.getName().getString(), name);
            } else {
                player.sendSystemMessage(
                        Component.translatable("message.abysscore.dimen.create_failed", name));
            }
        });
    }
}
