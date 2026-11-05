package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishMessages;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * "X has made the advancement [...]" → only staff, see VanishMessages.
 *
 * The handlers take ONLY the CallbackInfoReturnable (Mixin allows leaving out the target's
 * parameters). award() takes an AdvancementHolder in 1.21.1 but an Advancement in 1.20.1,
 * so not naming the parameters makes this mixin work on both.
 */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsVanishMixin {

    @Shadow private ServerPlayer player;

    @Inject(method = "award", at = @At("HEAD"))
    private void abysscore$awardStart(CallbackInfoReturnable<Boolean> cir) {
        VanishMessages.push(this.player);
    }

    @Inject(method = "award", at = @At("RETURN"))
    private void abysscore$awardEnd(CallbackInfoReturnable<Boolean> cir) {
        VanishMessages.pop();
    }
}
