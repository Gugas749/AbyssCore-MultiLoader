package com.gugas749.abysscore.api.effects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * Keeps registered effects on entities permanently (refreshes them before they expire).
 *
 *   AbyssEffectHandler.registerContinuousEffect(player.getUUID(),
 *           new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
 *   AbyssEffectHandler.unregisterContinuousEffect(player.getUUID(), MobEffects.NIGHT_VISION instance);
 *
 * Server side. Not saved: register again after a restart (e.g. on login).
 * Cleanup is automatic: players on logout, other entities when they die or are removed for good.
 * The loader listeners call the instance methods at the bottom; only plain methods here.
 */
public class AbyssEffectHandler {

    // UUID → effects to keep active on this entity (at most one per effect type)
    private static final Map<UUID, List<MobEffectInstance>> continuousEffects = new HashMap<>();

    /** Re-apply when fewer than this many ticks are left (or half the duration, if shorter). */
    private static final int REFRESH_THRESHOLD = 200;

    // ── API ───────────────────────────────────────────────────────────────────

    /**
     * Keeps these effects on the entity. Registering an effect type that is already registered
     * for this entity REPLACES the old instance (e.g. to change its amplifier).
     */
    public static void registerContinuousEffect(UUID target, List<MobEffectInstance> effects) {
        List<MobEffectInstance> list = continuousEffects.computeIfAbsent(target, k -> new ArrayList<>());
        for (MobEffectInstance effect : effects) {
            list.removeIf(e -> e.getEffect().equals(effect.getEffect()));
            list.add(effect);
        }
    }

    public static void registerContinuousEffect(UUID target, MobEffectInstance effect) {
        registerContinuousEffect(target, List.of(effect));
    }

    /**
     * Stops refreshing every effect of this entity. The effects already on the entity are NOT
     * removed — they run out on their own (call entity.removeEffect(...) to end them now).
     */
    public static void unregisterEveryContinuousEffect(UUID target) {
        continuousEffects.remove(target);
    }

    /** Stops refreshing that effect TYPE (any instance of the same effect works as the argument). */
    public static void unregisterContinuousEffect(UUID target, MobEffectInstance effect) {
        List<MobEffectInstance> effects = continuousEffects.get(target);
        if (effects != null) {
            effects.removeIf(e -> e.getEffect().equals(effect.getEffect()));
            if (effects.isEmpty()) {
                continuousEffects.remove(target);
            }
        }
    }

    /** True if this effect type is registered as continuous for the entity. */
    public static boolean hasContinuousEffect(UUID target, MobEffectInstance effect) {
        List<MobEffectInstance> effects = continuousEffects.get(target);
        return effects != null && effects.stream().anyMatch(e -> e.getEffect().equals(effect.getEffect()));
    }

    /** Read-only copy of the effects registered for the entity (empty if none). */
    public static List<MobEffectInstance> getContinuousEffects(UUID target) {
        List<MobEffectInstance> effects = continuousEffects.get(target);
        return effects == null ? List.of() : List.copyOf(effects);
    }

    // ── Events (called from the loader listeners) ─────────────────────────────

    /** Every living-entity tick. Server only: the client gets the effects from the server. */
    public void onEntityTick(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        List<MobEffectInstance> effects = continuousEffects.get(entity.getUUID());
        if (effects == null) return;

        // A dying mob won't come back: forget it. (Players keep theirs: they respawn with the same UUID.)
        if (!(entity instanceof Player) && entity.isDeadOrDying()) {
            continuousEffects.remove(entity.getUUID());
            return;
        }

        for (MobEffectInstance ce : effects) {
            var current = entity.getEffect(ce.getEffect());
            if (current == null) {
                entity.addEffect(new MobEffectInstance(ce));
                continue;
            }
            if (current.isInfiniteDuration()) continue;   // nothing to refresh (was re-applied every tick)

            // Short effects (e.g. 100 ticks) refresh at half their duration instead of EVERY tick.
            int threshold = ce.isInfiniteDuration() ? REFRESH_THRESHOLD
                    : Math.min(REFRESH_THRESHOLD, Math.max(1, ce.getDuration() / 2));
            if (current.getDuration() < threshold) {
                entity.addEffect(new MobEffectInstance(ce));
            }
        }
    }

    /** Player logout. */
    public void onPlayerLeave(Player player) {
        continuousEffects.remove(player.getUUID());
    }

    /**
     * An entity left a level. Only forget non-players removed FOR GOOD (killed, discarded):
     * an entity in an unloaded chunk comes back later with the same UUID, and a player changing
     * dimension also "leaves" a level.
     */
    public void onEntityLeaveLevel(Entity entity) {
        if (entity.level().isClientSide() || entity instanceof Player) return;
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason != null && reason.shouldDestroy()) {
            continuousEffects.remove(entity.getUUID());
        }
    }
}
