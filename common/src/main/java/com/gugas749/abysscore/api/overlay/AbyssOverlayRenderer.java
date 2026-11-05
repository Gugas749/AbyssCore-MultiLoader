package com.gugas749.abysscore.api.overlay;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.api.network.AbyssPacketContext;
import com.gugas749.abysscore.platform.Services;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * CLIENT: draws every active overlay (ABY-15). You never call this directly —
 * use AbyssScreenOverlay on the server / in common code.
 *
 * Register this renderer as a GUI layer via NeoForgeClientEventListeners on RegisterGuiLayersEvent.
 * Call onLoggingOut() from the logout event to clear active overlays.
 */
public final class AbyssOverlayRenderer {

    public static final ResourceLocation LAYER_ID = Constants.asResource("screen_overlay");

    private static final float MS_PER_TICK = 50f;

    private static final Map<ResourceLocation, Timed> TIMED = new HashMap<>();

    private static final class Timed {
        final AbyssOverlaySpec spec;
        final long startMs;
        float durationTicks;

        Timed(AbyssOverlaySpec spec, long startMs, float durationTicks) {
            this.spec = spec;
            this.startMs = startMs;
            this.durationTicks = durationTicks;
        }

        float elapsedTicks(long now) {
            return (now - startMs) / MS_PER_TICK;
        }
    }

    private AbyssOverlayRenderer() {}

    // ── Packet handler ────────────────────────────────────────────────────────

    public static void handle(ScreenOverlayPacket packet, AbyssPacketContext ctx) {
        ctx.enqueueWork(() -> {
            long now = Util.getMillis();
            if (packet.durationTicks() > 0) {
                TIMED.put(packet.id(), new Timed(packet.spec(), now, packet.durationTicks()));
                return;
            }
            Timed t = TIMED.get(packet.id());
            if (t != null) {
                t.durationTicks = Math.min(t.durationTicks, t.elapsedTicks(now) + t.spec.fadeOutTicks());
            }
        });
    }

    /** Called by NeoForgeClientEventListeners on player logout. */
    public static void onLoggingOut() {
        TIMED.clear();
    }

    // ── Render (called by the loader's client listener after the HUD) ─────────
    // No DeltaTracker parameter (it only exists in 1.21): the fades use real time (Util.getMillis).

    public static void render(GuiGraphics g) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        long now = Util.getMillis();

        Iterator<Timed> it = TIMED.values().iterator();
        while (it.hasNext()) {
            Timed t = it.next();
            float elapsed = t.elapsedTicks(now);
            float remaining = t.durationTicks - elapsed;
            if (remaining <= 0) { it.remove(); continue; }

            float fadeIn  = t.spec.fadeInTicks()  > 0 ? Math.min(1f, elapsed   / t.spec.fadeInTicks())  : 1f;
            float fadeOut = t.spec.fadeOutTicks() > 0 ? Math.min(1f, remaining / t.spec.fadeOutTicks()) : 1f;
            draw(g, t.spec, t.spec.maxAlpha() * Math.min(fadeIn, fadeOut));
        }

        var bindings = AbyssScreenOverlay.effectOverlays();
        if (bindings.isEmpty()) return;
        for (MobEffectInstance inst : player.getActiveEffects()) {
            // getEffect() is a Holder on 1.21.1 and a MobEffect on 1.20.1 → the loader gives us the id
            ResourceLocation effectId = Services.PLATFORM.getEffectId(inst);
            if (effectId == null) continue;
            AbyssOverlaySpec spec = bindings.get(effectId);
            if (spec == null) continue;

            float strength = spec.ampScaling() ? Math.min(1f, (inst.getAmplifier() + 1) / 4f) : 1f;
            float fadeOut = 1f;
            if (!inst.isInfiniteDuration() && spec.fadeOutTicks() > 0) {
                fadeOut = Math.min(1f, inst.getDuration() / (float) spec.fadeOutTicks());
            }
            draw(g, spec, spec.maxAlpha() * strength * fadeOut);
        }
    }

    private static void draw(GuiGraphics g, AbyssOverlaySpec spec, float alpha) {
        if (alpha <= 0.003f) return;
        int w = g.guiWidth(), h = g.guiHeight();
        int full = argb(spec.color(), alpha);

        if (spec.style() == AbyssOverlaySpec.Style.TINT) {
            g.fill(0, 0, w, h, full);
            return;
        }

        int clear = argb(spec.color(), 0f);
        int bandY = h / 4;
        g.fillGradient(0, 0, w, bandY, full, clear);
        g.fillGradient(0, h - bandY, w, h, clear, full);

        int bandX = w / 5, steps = 16;
        float stripW = bandX / (float) steps;
        for (int i = 0; i < steps; i++) {
            int c = argb(spec.color(), alpha * (1f - i / (float) steps));
            int x0 = Math.round(i * stripW), x1 = Math.round((i + 1) * stripW);
            g.fill(x0, 0, x1, h, c);
            g.fill(w - x1, 0, w - x0, h, c);
        }
    }

    private static int argb(int rgb, float alpha) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
