package com.gugas749.abysscore.api.damages;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class AbyssDamage {

    public static void deal(LivingEntity target, DamageSource source, float damage) {
        target.hurt(source, damage);
    }

    /**
     * Duration example: 100 is 5 seconds
     * Amplifier example: if the amplifier value given is 0 then the effect level is 1
     *                    if the amplifier value given is 1 then the effect level is 2
     */
    /**
     * Damage + an effect if the hit landed:
     *   AbyssDamage.deal(target, source, 4f, new MobEffectInstance(MobEffects.WITHER, 100, 1));
     * Takes a ready MobEffectInstance (not effect/duration/amplifier) because the effect type is
     * a Holder<MobEffect> on 1.21.1 and a MobEffect on 1.20.1 — but
     * "new MobEffectInstance(MobEffects.X, ...)" is the same source code on both.
     */
    public static void deal(LivingEntity target, DamageSource source, float damage, MobEffectInstance effect) {
        if (target.hurt(source, damage)) {
            target.addEffect(effect);
        }
    }
}
