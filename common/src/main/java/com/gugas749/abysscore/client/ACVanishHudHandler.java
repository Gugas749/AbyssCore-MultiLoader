package com.gugas749.abysscore.client;

import com.gugas749.abysscore.api.network.AbyssPacketContext;
import com.gugas749.abysscore.network.vanish.VanishStateSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * CLIENT: renders the vanish HUD warning and handles VanishStateSyncPacket.
 * The loader calls render() after the HUD is drawn (NeoForge/Forge: RenderGuiEvent.Post,
 * wired in each Abysscore.java). Only plain methods here.
 */
public class ACVanishHudHandler {

    private static boolean isVanished = false;
    private static boolean showHud = true;     // the player's HUD_WARNING option
    private static long vanishedSince = 0;

    public static void handleSync(VanishStateSyncPacket packet, AbyssPacketContext ctx) {
        ctx.enqueueWork(() -> {
            boolean wasVanished = isVanished;
            isVanished = packet.vanished();
            showHud = packet.showHud();
            if (!wasVanished && isVanished) {
                vanishedSince = System.currentTimeMillis();
            }
        });
    }

    /** Called by the loader on RenderGuiEvent.Post. */
    public void render(GuiGraphics graphics, Minecraft mc) {
        if (!isVanished || !showHud) return;
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.screen != null) return;

        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        Component text = Component.translatable("message.abysscore.vanish.hud_warning");
        int textWidth = mc.font.width(text);

        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 52;

        long elapsed = System.currentTimeMillis() - vanishedSince;
        float pulse = (float) Math.sin(elapsed / 600.0) * 0.5f + 0.5f;
        int alpha = (int) (180 + pulse * 75);

        int padding = 4;
        graphics.fill(
            x - padding,
            y - padding,
            x + textWidth + padding,
            y + mc.font.lineHeight + padding,
            (alpha / 2) << 24
        );

        int color = (alpha << 24) | 0xFF4444;
        graphics.drawString(mc.font, text, x, y, color, false);
    }

    public static boolean isVanished() { return isVanished; }
}
