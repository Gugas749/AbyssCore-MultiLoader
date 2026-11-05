package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishManager;
import com.gugas749.abysscore.features.vanish.VanishOption;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NO_COLLISION. Two directions:
 *   isPushable() = false → mobs walking into you don't push you.
 *   pushEntities() skipped → you don't push mobs (a mob sliding away from "nothing" is a giveaway).
 *
 * Targets LivingEntity, not ServerPlayer: ServerPlayer.pushEntities() only exists in newer
 * versions and calls super anyway, so hooking LivingEntity works on 1.20.1 and 1.21.1.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityVanishMixin {

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void abysscore$notPushable(CallbackInfoReturnable<Boolean> cir) {
        if (VanishManager.isActive((LivingEntity) (Object) this, VanishOption.NO_COLLISION)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void abysscore$dontPush(CallbackInfo ci) {
        if (VanishManager.isActive((LivingEntity) (Object) this, VanishOption.NO_COLLISION)) {
            ci.cancel();
        }
    }
}
