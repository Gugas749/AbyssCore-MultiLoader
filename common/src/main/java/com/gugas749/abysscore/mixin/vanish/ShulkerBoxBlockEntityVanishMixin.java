package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishHooks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SILENT_CONTAINERS for shulker boxes. They don't use ContainerOpenersCounter like chests:
 * they keep their own openCount in startOpen() / stopOpen(), and those methods send the lid
 * animation (blockEvent), the sculk game event and the sound. A silent opener is never counted.
 */
@Mixin(ShulkerBoxBlockEntity.class)
public abstract class ShulkerBoxBlockEntityVanishMixin {

    @Shadow private int openCount;

    @Inject(method = "startOpen", at = @At("HEAD"), cancellable = true)
    private void abysscore$silentOpen(Player player, CallbackInfo ci) {
        if (VanishHooks.isSilentOpener(player)) ci.cancel();
    }

    @Inject(method = "stopOpen", at = @At("HEAD"), cancellable = true)
    private void abysscore$silentClose(Player player, CallbackInfo ci) {
        // openCount <= 0: opened silently, then unvanished before closing. Without this
        // guard the box would play a close sound for a box nobody saw open.
        if (VanishHooks.isSilentOpener(player) || this.openCount <= 0) ci.cancel();
    }
}
