package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishManager;
import com.gugas749.abysscore.features.vanish.VanishOption;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NO_PICKUP for XP orbs. Two parts:
 *
 *   1. Following: once a second an orb runs scanForEntities() and stores the player it will
 *      fly to in followingPlayer. Orbs flying towards "nothing" give a vanished player away,
 *      so if the chosen player is a vanished one we pick the nearest player that ISN'T
 *      (or nobody, and the orb just stays on the ground).
 *   2. Touching: playerTouch() is where the XP goes into the player; we skip it.
 */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbVanishMixin {

    @Shadow private Player followingPlayer;

    @Inject(method = "scanForEntities", at = @At("TAIL"))
    private void abysscore$dontFollowVanished(CallbackInfo ci) {
        if (!VanishManager.isActive(this.followingPlayer, VanishOption.NO_PICKUP)) return;

        ExperienceOrb self = (ExperienceOrb) (Object) this;
        // 8 blocks = the same range vanilla uses to find a player
        this.followingPlayer = self.level().getNearestPlayer(self.getX(), self.getY(), self.getZ(), 8.0,
                entity -> !entity.isSpectator() && !VanishManager.isActive(entity, VanishOption.NO_PICKUP));
    }

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void abysscore$noPickup(Player player, CallbackInfo ci) {
        if (VanishManager.isActive(player, VanishOption.NO_PICKUP)) {
            ci.cancel();
        }
    }
}
