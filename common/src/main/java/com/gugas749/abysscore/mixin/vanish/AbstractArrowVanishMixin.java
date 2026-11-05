package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishManager;
import com.gugas749.abysscore.features.vanish.VanishOption;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NO_PICKUP for arrows and tridents stuck in the ground. */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowVanishMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void abysscore$noPickup(Player player, CallbackInfo ci) {
        if (VanishManager.isActive(player, VanishOption.NO_PICKUP)) {
            ci.cancel();
        }
    }
}
