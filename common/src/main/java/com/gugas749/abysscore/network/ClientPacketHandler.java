package com.gugas749.abysscore.network;

import com.gugas749.abysscore.api.network.AbyssPacketContext;
import com.gugas749.abysscore.client.ui.AbyssCoreMenuScreen;
import com.gugas749.abysscore.client.ui.screens.DimenCreateScreen;
import com.gugas749.abysscore.client.ui.screens.RegionManagerScreen;
import com.gugas749.abysscore.network.dimen.OpenDimenCreateScreenPacket;
import com.gugas749.abysscore.network.menu.packets.OpenMainMenuPacket;
import com.gugas749.abysscore.network.region.OpenRegionScreenPacket;
import net.minecraft.client.Minecraft;

/**
 * CLIENT-side packet handlers for S2C packets that open screens.
 * Event wiring (NeoForge) lives in NeoForgeClientEventListeners.
 */
public class ClientPacketHandler {

    public static void handleOpenDimenCreate(OpenDimenCreateScreenPacket pkt, AbyssPacketContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(new DimenCreateScreen()));
    }

    public static void handleOpenRegionScreen(OpenRegionScreenPacket pkt, AbyssPacketContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(new RegionManagerScreen(pkt.regions())));
    }

    public static void handleOpenMainMenu(OpenMainMenuPacket pkt, AbyssPacketContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(new AbyssCoreMenuScreen(pkt)));
    }
}
