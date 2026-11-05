package com.gugas749.abysscore.api.damages;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Builds DamageSources from damage type keys. Works on 1.20.1 and 1.21.1.
 *
 *   AbyssDamageSources.corruption(level)                              // no attacker
 *   AbyssDamageSources.corruption(level, caster)                      // with an attacker (kill credit)
 *   AbyssDamageSources.source(MyDamageTypes.CURSE, level)             // YOUR damage types
 *   AbyssDamageSources.source(MyDamageTypes.CURSE, level, attacker)
 *   AbyssDamageSources.source(MyDamageTypes.CURSE, level, arrow, shooter)
 *
 * The damage type must exist as data (data/<namespace>/damage_type/<path>.json, or datagen with
 * DamageTypeBuilder), otherwise these throw: the registry has no entry for the key.
 */
// Parameters stay Level (not LevelReader): corruption(Level) existed in 0.1.0 and mods compiled
// against it call that exact signature.
public class AbyssDamageSources {

    // ── AbyssCore's own types ─────────────────────────────────────────────────

    public static DamageSource corruption(Level level) {
        return source(AbyssDamageTypes.CORRUPTION_DAMAGE, level);
    }

    /** Corruption caused by an entity (a skill, a mob): the ".player" death message names it. */
    public static DamageSource corruption(Level level, @Nullable Entity attacker) {
        return source(AbyssDamageTypes.CORRUPTION_DAMAGE, level, attacker);
    }

    // ── Any damage type ───────────────────────────────────────────────────────

    /** No attacker (environment-style damage). */
    public static DamageSource source(ResourceKey<DamageType> key, Level level) {
        return new DamageSource(holder(key, level));
    }

    /** {@code attacker} is both the direct and the causing entity (a melee hit, a spell). */
    public static DamageSource source(ResourceKey<DamageType> key, Level level, @Nullable Entity attacker) {
        return new DamageSource(holder(key, level), attacker);
    }

    /**
     * Projectile-style damage.
     * @param directEntity  what actually hit (the arrow, the fireball)
     * @param causingEntity who is responsible (the shooter) — gets the kill credit
     */
    public static DamageSource source(ResourceKey<DamageType> key, Level level,
                                      @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        // DamageSource(type, directEntity, causingEntity) — same parameter order on both versions
        return new DamageSource(holder(key, level), directEntity, causingEntity);
    }

    private static net.minecraft.core.Holder<DamageType> holder(ResourceKey<DamageType> key, Level level) {
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        return registry.getHolderOrThrow(key);
    }
}
