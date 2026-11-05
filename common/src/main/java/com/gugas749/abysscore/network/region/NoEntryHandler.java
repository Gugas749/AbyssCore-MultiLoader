package com.gugas749.abysscore.network.region;

import com.gugas749.abysscore.api.network.AbyssPacketContext;
import com.gugas749.abysscore.network.AbyssNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * CLIENT: renders the black fade-out when ejected from a no_entry region.
 * Event wiring (NeoForge) lives in NeoForgeClientEventListeners; only plain methods here.
 */
public class NoEntryHandler {

    private static long effectStartMs = -1;
    private static boolean teleportSent = false;

    private static final long FADE_IN_MS  = 400;
    private static final long HOLD_MS     = 200;
    private static final long FADE_OUT_MS = 400;
    private static final long TOTAL_MS    = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;

    public static void handlePacket(NoEntryPacket packet, AbyssPacketContext ctx) {
        ctx.enqueueWork(() -> {
            effectStartMs = System.currentTimeMillis();
            teleportSent = false;
        });
    }

    /** Called by NeoForgeClientEventListeners on RenderGuiEvent.Post. */
    public void onRenderGui(GuiGraphics g) {
        if (effectStartMs < 0) return;

        long elapsed = System.currentTimeMillis() - effectStartMs;
        if (elapsed >= TOTAL_MS) {
            effectStartMs = -1;
            teleportSent = false;
            return;
        }

        int alpha;
        if (elapsed < FADE_IN_MS) {
            alpha = (int) (255 * elapsed / FADE_IN_MS);
        } else if (elapsed < FADE_IN_MS + HOLD_MS) {
            alpha = 255;
        } else {
            long fadeElapsed = elapsed - FADE_IN_MS - HOLD_MS;
            alpha = 255 - (int) (255 * fadeElapsed / FADE_OUT_MS);
        }

        alpha = Math.max(0, Math.min(255, alpha));

        if (alpha == 255 && !teleportSent) {
            teleportSent = true;
            AbyssNetwork.sendToServer(new ReadyToTeleportPacket());
        }

        Minecraft mc = Minecraft.getInstance();
        g.fill(0, 0,
            mc.getWindow().getGuiScaledWidth(),
            mc.getWindow().getGuiScaledHeight(),
            (alpha << 24) | 0x000000
        );
    }
}
