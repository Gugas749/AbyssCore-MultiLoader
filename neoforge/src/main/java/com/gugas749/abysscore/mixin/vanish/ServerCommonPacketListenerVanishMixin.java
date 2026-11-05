package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishHooks;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * Every packet the server sends to a player goes through send(packet, listener).
 * We let VanishHooks.filterOutgoing look at it first (it only changes tab-list packets).
 *
 * NeoForge 1.21.1 only (it's in the neoforge module): ServerCommonPacketListenerImpl was
 * added in 1.20.2. The Forge 1.20.1 version is forge/.../ServerGamePacketListenerSendVanishMixin.
 */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerVanishMixin {

    @Shadow
    public abstract void send(Packet<?> packet, @Nullable PacketSendListener listener);

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
            at = @At("HEAD"), cancellable = true)
    private void abysscore$filterVanished(Packet<?> packet, @Nullable PacketSendListener listener, CallbackInfo ci) {
        // Only the "play" phase has a player (configuration phase listeners don't)
        if (!((Object) this instanceof ServerGamePacketListenerImpl game)) return;

        Packet<?> filtered = VanishHooks.filterOutgoing(game.player, packet);
        if (filtered == packet) return;          // untouched → vanilla continues

        ci.cancel();                             // drop the original...
        if (filtered != null) {
            this.send(filtered, listener);       // ...and send the filtered copy (passes here again, unchanged)
        }
    }
}
