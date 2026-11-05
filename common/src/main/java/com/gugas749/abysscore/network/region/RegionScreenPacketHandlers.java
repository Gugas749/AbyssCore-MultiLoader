package com.gugas749.abysscore.network.region;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.api.network.AbyssPacketContext;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import com.gugas749.abysscore.features.regions.ACRegionSavedData;
import com.gugas749.abysscore.network.AbyssNetwork;
import com.gugas749.abysscore.network.menu.packets.RequestRegionScreenPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;

public class RegionScreenPacketHandlers {

    /**
     * C2S: client requests the region manager screen.
     * Server builds the region list and sends OpenRegionScreenPacket back.
     * Extracted from the inline lambda in PacketHandler.
     */
    public static void handleRequest(RequestRegionScreenPacket packet, AbyssPacketContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer op)) return;
            if (!AbyssPermissionHandler.require(op, AbyssPermissionLevel.ADMIN)) return;   // regions are ADMIN (same as /abysscore region)

            var data = ACRegionSavedData.get(op.serverLevel());
            var entries = new ArrayList<OpenRegionScreenPacket.RegionEntry>();

            for (var region : data.regions()) {
                entries.add(new OpenRegionScreenPacket.RegionEntry(
                    region.name(), region.dimension(),
                    region.minX(), region.minY(), region.minZ(),
                    region.maxX(), region.maxY(), region.maxZ(),
                    region.tags(), region.entryFilterTag()
                ));
            }

            AbyssNetwork.sendToPlayer(op, new OpenRegionScreenPacket(entries));
        });
    }

    /**
     * C2S: client submits updated tags / delete for a region.
     */
    public static void handleRegionUpdate(SubmitRegionUpdatePacket packet, AbyssPacketContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!AbyssPermissionHandler.require(player, AbyssPermissionLevel.ADMIN)) return;

            ACRegionSavedData data = ACRegionSavedData.get(player.serverLevel());
            String name = packet.regionName();

            if (packet.delete()) {
                if (data.removeRegion(name)) {
                    player.sendSystemMessage(
                        Component.translatable("message.abysscore.region.removed", name));
                    Constants.LOG.info("[AbyssCore] {} deleted region '{}'.",
                        player.getName().getString(), name);
                }
                return;
            }

            data.getRegion(name).ifPresent(region -> {
                new java.util.HashSet<>(region.tags()).forEach(tag ->
                    data.removeTagFromRegion(name, tag));
                packet.tags().forEach(tag -> data.addTagToRegion(name, tag));
                data.setEntryFilterTag(name, packet.entryFilterTag());

                player.sendSystemMessage(
                    Component.translatable("message.abysscore.region.screen_saved", name));
                Constants.LOG.info("[AbyssCore] {} updated region '{}' — tags: {}, filter: '{}'",
                    player.getName().getString(), name, packet.tags(), packet.entryFilterTag());
            });
        });
    }
}
