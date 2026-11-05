package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishHooks;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Server list: vanished players don't count in "5/20" and aren't in the hover list. */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerVanishMixin {

    @Shadow
    public abstract PlayerList getPlayerList();

    @Inject(method = "buildPlayerStatus", at = @At("RETURN"), cancellable = true)
    private void abysscore$hideFromServerList(CallbackInfoReturnable<ServerStatus.Players> cir) {
        ServerStatus.Players filtered = VanishHooks.filterStatus(this.getPlayerList(), cir.getReturnValue());
        if (filtered != null) cir.setReturnValue(filtered);
    }
}
