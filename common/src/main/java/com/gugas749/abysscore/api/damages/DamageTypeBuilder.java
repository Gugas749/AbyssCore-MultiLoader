package com.gugas749.abysscore.api.damages;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DeathMessageType;
import net.minecraft.world.entity.Entity;

public class DamageTypeBuilder {
    protected final ResourceKey<DamageType> key;

    protected String msgId;
    protected DamageScaling scaling;
    protected float exhaustion = 0.0f;
    protected DamageEffects effects;
    protected DeathMessageType deathMessageType;

    public DamageTypeBuilder(ResourceKey<DamageType> key) {
        this.key = key;
    }

//    public DamageTypeBuilder msgId(String msgId) {
//        this.msgId = msgId;
//        return this;
//    }
//    public DamageTypeBuilder simpleMsgId() {
//        return msgId(key.location().getNamespace() + "." + key.location().getPath());
//    }

    /**
     * Set the scaling of this type. This determines whether damage is increased based on difficulty or not.
     */
    public DamageTypeBuilder scaling(DamageScaling scaling) {
        this.scaling = scaling;
        return this;
    }

    /**
     * Set the exhaustion of this type. This is the amount of hunger that will be consumed when an entity is damaged.
     */
    public DamageTypeBuilder exhaustion(float exhaustion) {
        this.exhaustion = exhaustion;
        return this;
    }

    /**
     * Set the effects of this type. This determines the sound that plays when damaged.
     */
    public DamageTypeBuilder effects(DamageEffects effects) {
        this.effects = effects;
        return this;
    }

    /**
     * Set the death message type of this damage type. This determines how a death message lang key is assembled.
     * <ul>
     *     <li>{@link DeathMessageType#DEFAULT}: {@link DamageSource#getLocalizedDeathMessage}</li>
     *     <li>{@link DeathMessageType#FALL_VARIANTS}: {@link CombatTracker#getFallMessage(CombatEntry, Entity)}</li>
     *     <li>{@link DeathMessageType#INTENTIONAL_GAME_DESIGN}: "death.attack." + msgId, wrapped in brackets, linking to MCPE-28723</li>
     * </ul>
     */
    public DamageTypeBuilder deathMessageType(DeathMessageType deathMessageType) {
        this.deathMessageType = deathMessageType;
        return this;
    }

    public DamageType build() {
        if (msgId == null) {
            this.msgId = key.location().getNamespace() + "." + key.location().getPath();
        }
        if (scaling == null) {
            scaling(DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER);
        }
        if (effects == null) {
            effects(DamageEffects.HURT);
        }
        if (deathMessageType == null) {
            deathMessageType(DeathMessageType.DEFAULT);
        }
        return new DamageType(msgId, scaling, exhaustion, effects, deathMessageType);
    }

    /**
     * Registers the built type. In datagen pass the bootstrap context's register method:
     *   builder.register(ctx::register);
     * (Taking a BiConsumer instead of BootstrapContext keeps this class version-neutral:
     *  the context class is BootstapContext on 1.20.1 and BootstrapContext on 1.21.1.)
     */
    public DamageType register(java.util.function.BiConsumer<ResourceKey<DamageType>, DamageType> registerer) {
        DamageType type = build();
        registerer.accept(key, type);
        return type;
    }
}
