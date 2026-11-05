package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishManager;
import com.gugas749.abysscore.features.vanish.VanishMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerVanishMixin {

    /**
     * THE core of vanish. Before the server sends an entity to a client (spawn, movement,
     * equipment...), ChunkMap asks entity.broadcastToPlayer(viewer). Answering false means
     * that client never gets the body. VanishManager.refresh() re-runs this check on toggle.
     */
    @Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
    private void abysscore$hideFromViewer(ServerPlayer viewer, CallbackInfoReturnable<Boolean> cir) {
        if (!VanishManager.canSee(viewer, (ServerPlayer) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    /** Death message → only staff, see VanishMessages. */
    @Inject(method = "die", at = @At("HEAD"))
    private void abysscore$deathStart(DamageSource source, CallbackInfo ci) {
        VanishMessages.push((ServerPlayer) (Object) this);
    }

    @Inject(method = "die", at = @At("RETURN"))
    private void abysscore$deathEnd(DamageSource source, CallbackInfo ci) {
        VanishMessages.pop();
    }
}
