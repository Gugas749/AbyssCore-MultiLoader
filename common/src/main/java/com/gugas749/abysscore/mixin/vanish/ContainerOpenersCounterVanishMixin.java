package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

/**
 * SILENT_CONTAINERS. Chests, trapped chests, barrels and ender chests count their viewers
 * here. The lid animation + sound play when the count goes 0 → 1 (and 1 → 0 on close).
 * A silent opener is simply never counted. (Shulker boxes don't use this class: see
 * ShulkerBoxBlockEntityVanishMixin.)
 */
@Mixin(ContainerOpenersCounter.class)
public abstract class ContainerOpenersCounterVanishMixin {

    @Shadow private int openCount;

    @Inject(method = "incrementOpeners", at = @At("HEAD"), cancellable = true)
    private void abysscore$silentOpen(Player player, Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (VanishHooks.isSilentOpener(player)) ci.cancel();
    }

    @Inject(method = "decrementOpeners", at = @At("HEAD"), cancellable = true)
    private void abysscore$silentClose(Player player, Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        // openCount <= 0: the player opened it silently, then unvanished before closing.
        // Without this guard the count would go to -1 and the chest would stop animating.
        if (VanishHooks.isSilentOpener(player) || this.openCount <= 0) ci.cancel();
    }

    /**
     * Every 5 ticks the counter re-counts the players looking inside, with
     * level.getEntities(Player type, box around the chest, filter). We add "and not a silent
     * opener" to that filter (argument index 2).
     *
     * The method doing the re-count is getPlayersWithContainerOpen on 1.21.1 and getOpenCount
     * on 1.20.1; both are listed, so the same mixin works on both versions. Mixin only fails
     * if NONE of them matches.
     */
    @ModifyArg(method = {"getPlayersWithContainerOpen", "getOpenCount"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"),
            index = 2)
    private Predicate<Player> abysscore$skipSilentOpeners(Predicate<Player> original) {
        return player -> original.test(player) && !VanishHooks.isSilentOpener(player);
    }
}
