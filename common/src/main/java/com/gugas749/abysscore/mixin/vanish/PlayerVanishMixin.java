package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishManager;
import com.gugas749.abysscore.features.vanish.VanishOption;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MOBS_IGNORE. Mobs only pick (and keep) a target that canBeSeenAsEnemy() — it's what makes
 * them ignore creative players. Target goals, brain sensors (piglins, wardens...) and
 * Mob.canAttack() all ask it, so a mob already chasing you also gives up.
 */
@Mixin(Player.class)
public abstract class PlayerVanishMixin {

    @Inject(method = "canBeSeenAsEnemy", at = @At("HEAD"), cancellable = true)
    private void abysscore$mobsIgnore(CallbackInfoReturnable<Boolean> cir) {
        if (VanishManager.isActive((Player) (Object) this, VanishOption.MOBS_IGNORE)) {
            cir.setReturnValue(false);
        }
    }
}
