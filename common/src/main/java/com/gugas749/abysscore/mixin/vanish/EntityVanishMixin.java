package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishManager;
import com.gugas749.abysscore.features.vanish.VanishOption;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NO_TRIGGERS (part 1). Pressure plates and tripwires skip entities whose
 * isIgnoringBlockTriggers() is true (vanilla uses it for armor stands and markers).
 * Sculk / vibrations are handled in ServerLevelVanishMixin.
 */
@Mixin(Entity.class)
public abstract class EntityVanishMixin {

    @Inject(method = "isIgnoringBlockTriggers", at = @At("HEAD"), cancellable = true)
    private void abysscore$ignoreTriggers(CallbackInfoReturnable<Boolean> cir) {
        if (VanishManager.isActive((Entity) (Object) this, VanishOption.NO_TRIGGERS)) {
            cir.setReturnValue(true);
        }
    }
}
