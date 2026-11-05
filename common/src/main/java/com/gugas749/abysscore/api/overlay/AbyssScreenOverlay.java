package com.gugas749.abysscore.api.overlay;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.network.AbyssNetwork;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ABY-15 — screen overlays (tints / vignettes) triggered by effects, attacks, or code.
 *
 * Three ways to use it:
 *
 *  1. EFFECT → overlay (shown for as long as the player has the effect, no packets involved):
 *
 *       AbyssScreenOverlay.bindEffect(MyEffects.BLEEDING,
 *               AbyssOverlaySpec.vignette(0xAA0000, 0.5f).scaleWithAmplifier());
 *
 *     Call this from your mod constructor (or common setup) so it runs on BOTH the client and
 *     the server — the client is the one that draws it.
 *
 *  2. DAMAGE TYPE → overlay (flashes whenever a player takes that kind of damage):
 *
 *       AbyssScreenOverlay.bindDamageType(AbyssDamageTypes.CORRUPTION_DAMAGE,
 *               AbyssOverlaySpec.tint(0x5A0080, 0.35f).fade(2, 15), 20);
 *
 *  3. Directly from server code (a skill, a cutscene...):
 *
 *       AbyssScreenOverlay.show(player, MyMod.id("flash"), AbyssOverlaySpec.tint(0xFFFFFF, 0.8f), 10);
 *       AbyssScreenOverlay.clear(player, MyMod.id("flash"));
 *
 * Same id shown again = replaced (and its timer restarts), different ids stack.
 */
public final class AbyssScreenOverlay {

    /** Effect id → how it looks. Read by the client renderer every frame. */
    private static final Map<ResourceLocation, AbyssOverlaySpec> EFFECT_OVERLAYS = new HashMap<>();

    /** Damage type → (how it looks, how long). Read on the server when a player is hurt. */
    private static final Map<ResourceKey<DamageType>, DamageOverlay> DAMAGE_OVERLAYS = new HashMap<>();

    private record DamageOverlay(ResourceLocation id, AbyssOverlaySpec spec, int durationTicks) {}

    private AbyssScreenOverlay() {}

    // ── Bindings ──────────────────────────────────────────────────────────────

    /**
     * Works with vanilla effects (MobEffects.POISON) and your own DeferredHolder effects.
     * The holder MUST be a registered reference (it needs a registry key) — DeferredHolder is.
     */
    public static void bindEffect(Holder<MobEffect> effect, AbyssOverlaySpec spec) {
        ResourceKey<MobEffect> key = effect.unwrapKey().orElseThrow(() ->
                new IllegalArgumentException("[AbyssCore] bindEffect needs a registered effect holder"));
        EFFECT_OVERLAYS.put(key.location(), spec);
    }

    /**
     * By effect id — works on BOTH versions at any time, even before registries are filled
     * (so from the mod constructor on Forge too):
     *   AbyssScreenOverlay.bindEffect(Constants.id("mymod", "bleeding"), spec);
     */
    public static void bindEffect(ResourceLocation effectId, AbyssOverlaySpec spec) {
        EFFECT_OVERLAYS.put(effectId, spec);
    }

    /**
     * By plain MobEffect — for Forge 1.20.1, where MobEffects.X and RegistryObject.get() are
     * MobEffects, not Holders. The effect must already be REGISTERED (call it from common setup,
     * not the mod constructor), otherwise this throws.
     */
    public static void bindEffect(MobEffect effect, AbyssOverlaySpec spec) {
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        if (id == null) {
            throw new IllegalArgumentException("[AbyssCore] bindEffect: effect is not registered yet ("
                    + effect + ") — bind it in common setup, or use bindEffect(ResourceLocation, spec)");
        }
        EFFECT_OVERLAYS.put(id, spec);
    }

    /** Removes an effect binding. */
    public static void unbindEffect(ResourceLocation effectId) {
        EFFECT_OVERLAYS.remove(effectId);
    }

    public static void bindDamageType(ResourceKey<DamageType> type, AbyssOverlaySpec spec, int durationTicks) {
        ResourceLocation id = Constants.id(
                type.location().getNamespace(), "damage/" + type.location().getPath());
        DAMAGE_OVERLAYS.put(type, new DamageOverlay(id, spec, durationTicks));
    }

    /** Read-only view for the client renderer. */
    public static Map<ResourceLocation, AbyssOverlaySpec> effectOverlays() {
        return Collections.unmodifiableMap(EFFECT_OVERLAYS);
    }

    // ── Direct control (server side) ──────────────────────────────────────────

    public static void show(ServerPlayer player, ResourceLocation id, AbyssOverlaySpec spec, int durationTicks) {
        AbyssNetwork.sendToPlayer(player, new ScreenOverlayPacket(id, spec, Math.max(1, durationTicks)));
    }

    /** Fades the overlay out (using its fade-out time) instead of cutting it instantly. */
    public static void clear(ServerPlayer player, ResourceLocation id) {
        AbyssNetwork.sendToPlayer(player,
                new ScreenOverlayPacket(id, AbyssOverlaySpec.tint(0, 0f), 0));
    }

    // ── Damage hook ───────────────────────────────────────────────────────────

    /**
     * Called by the loader event listener after armor/resistances, only if damage went through.
     * Checks all bound damage types and sends the overlay if matched.
     */
    public static void onDamage(ServerPlayer player, DamageSource source, float newDamage) {
        if (DAMAGE_OVERLAYS.isEmpty()) return;
        if (newDamage <= 0) return;

        for (var entry : DAMAGE_OVERLAYS.entrySet()) {
            if (source.is(entry.getKey())) {
                DamageOverlay o = entry.getValue();
                show(player, o.id(), o.spec(), o.durationTicks());
            }
        }
    }
}
