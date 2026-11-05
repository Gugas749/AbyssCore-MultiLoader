package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishManager;
import com.gugas749.abysscore.features.vanish.VanishOption;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NO_TRIGGERS (part 2). Every "game event" (step, block placed, item used, container opened...)
 * goes through ServerLevel.gameEvent(). Sculk sensors, shriekers, wardens and allays listen to
 * them. Dropping the ones caused by a vanished player makes them all ignore you.
 *
 * NeoForge 1.21.1 only (it's in the neoforge module): the event is a Holder<GameEvent> here.
 * The Forge 1.20.1 copy (forge module) takes a plain GameEvent.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelVanishMixin {

    @Inject(method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",
            at = @At("HEAD"), cancellable = true)
    private void abysscore$noVibrations(Holder<GameEvent> event, Vec3 pos, GameEvent.Context context, CallbackInfo ci) {
        if (VanishManager.isActive(context.sourceEntity(), VanishOption.NO_TRIGGERS)) {
            ci.cancel();
        }
    }
}
