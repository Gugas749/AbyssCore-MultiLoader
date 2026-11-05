package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishManager;
import com.gugas749.abysscore.features.vanish.VanishOption;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NO_PICKUP: playerTouch() is where an item on the ground goes into the touching player's inventory. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityVanishMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void abysscore$noPickup(Player player, CallbackInfo ci) {
        if (VanishManager.isActive(player, VanishOption.NO_PICKUP)) {
            ci.cancel();
        }
    }
}
