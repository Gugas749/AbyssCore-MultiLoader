package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishHooks;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * Forge 1.20.1 copy of neoforge/.../ServerCommonPacketListenerVanishMixin: every packet sent
 * to a player passes through send(packet, listener), and VanishHooks.filterOutgoing removes
 * hidden players from tab-list packets.
 *
 * Why a copy: on 1.20.1 send() is declared in ServerGamePacketListenerImpl; 1.20.2 moved it
 * to the new parent class ServerCommonPacketListenerImpl, which doesn't exist here.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerSendVanishMixin {

    @Shadow public ServerPlayer player;

    @Shadow
    public abstract void send(Packet<?> packet, @Nullable PacketSendListener listener);

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
            at = @At("HEAD"), cancellable = true)
    private void abysscore$filterVanished(Packet<?> packet, @Nullable PacketSendListener listener, CallbackInfo ci) {
        Packet<?> filtered = VanishHooks.filterOutgoing(this.player, packet);
        if (filtered == packet) return;          // untouched → vanilla continues

        ci.cancel();                             // drop the original...
        if (filtered != null) {
            this.send(filtered, listener);       // ...and send the filtered copy (passes here again, unchanged)
        }
    }
}
