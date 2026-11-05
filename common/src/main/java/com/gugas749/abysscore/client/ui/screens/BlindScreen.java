package com.gugas749.abysscore.client.ui.screens;

import com.gugas749.abysscore.api.network.AbyssPacketContext;
import com.gugas749.abysscore.network.blind.BlindSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Renders a full opaque black overlay when the player is blinded.
 *
 * Rendered in BOTH Pre and Post events to cover everything —
 * including chat message backgrounds which are transparent in Post only.
 * Event wiring (NeoForge) lives in NeoForgeClientEventListeners; only plain methods here.
 */
public class BlindScreen {

    private static boolean blinded = false;

    public static void handleSync(BlindSyncPacket packet, AbyssPacketContext ctx) {
        ctx.enqueueWork(() -> blinded = packet.blinded());
    }

    public static boolean isBlinded() { return blinded; }

    /** Called on both Pre and Post render GUI events by NeoForgeClientEventListeners. */
    public static void renderBlack(GuiGraphics g) {
        if (!blinded) return;
        if (Minecraft.getInstance().player == null) return;
        Minecraft mc = Minecraft.getInstance();
        // 0xFF000000 — alpha MUST be FF (fully opaque), 0x000000 alone is transparent
        g.fill(0, 0,
            mc.getWindow().getGuiScaledWidth(),
            mc.getWindow().getGuiScaledHeight(),
            0xFF000000
        );
    }
}
