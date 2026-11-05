package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishManager;
import com.gugas749.abysscore.features.vanish.VanishOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge 1.20.1 copy of neoforge/.../ServerLevelVanishMixin (NO_TRIGGERS, part 2): drop game
 * events caused by a vanished player, so sculk sensors, shriekers, wardens and allays ignore
 * them. Only difference: on 1.20.1 the first parameter is a plain GameEvent, on 1.21.1 a
 * Holder<GameEvent>.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelVanishMixin {

    @Inject(method = "gameEvent(Lnet/minecraft/world/level/gameevent/GameEvent;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",
            at = @At("HEAD"), cancellable = true)
    private void abysscore$noVibrations(GameEvent event, Vec3 pos, GameEvent.Context context, CallbackInfo ci) {
        if (VanishManager.isActive(context.sourceEntity(), VanishOption.NO_TRIGGERS)) {
            ci.cancel();
        }
    }
}
