package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leave message → only staff, see VanishMessages.
 *
 * "X left the game" is sent while onDisconnect(...) runs: directly inside it on 1.20.1,
 * through removePlayerFromWorld() on 1.21.1. onDisconnect exists in both versions (with a
 * different parameter: Component vs DisconnectionDetails), so the handlers take ONLY the
 * CallbackInfo — Mixin allows leaving out all the target's parameters — and this one mixin
 * works on both.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerVanishMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void abysscore$leaveStart(CallbackInfo ci) {
        VanishMessages.push(this.player);
    }

    @Inject(method = "onDisconnect", at = @At("RETURN"))
    private void abysscore$leaveEnd(CallbackInfo ci) {
        VanishMessages.pop();
    }
}
