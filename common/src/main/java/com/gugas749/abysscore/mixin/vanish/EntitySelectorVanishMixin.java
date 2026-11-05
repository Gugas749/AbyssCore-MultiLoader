package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishHooks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Command targets (@a, @p, @e, player names). findSinglePlayer() calls findPlayers(), so
 * "/msg VanishedGuy hi" answers "No player was found" like they are offline.
 */
@Mixin(EntitySelector.class)
public abstract class EntitySelectorVanishMixin {

    @Inject(method = "findPlayers", at = @At("RETURN"), cancellable = true)
    private void abysscore$hidePlayers(CommandSourceStack source, CallbackInfoReturnable<List<ServerPlayer>> cir) {
        List<ServerPlayer> filtered = VanishHooks.filterSelected(source, cir.getReturnValue());
        if (filtered != null) cir.setReturnValue(filtered);
    }

    @Inject(method = "findEntities", at = @At("RETURN"), cancellable = true)
    private void abysscore$hideEntities(CommandSourceStack source, CallbackInfoReturnable<List<? extends Entity>> cir) {
        List<? extends Entity> filtered = VanishHooks.filterSelected(source, cir.getReturnValue());
        if (filtered != null) cir.setReturnValue(filtered);
    }
}
