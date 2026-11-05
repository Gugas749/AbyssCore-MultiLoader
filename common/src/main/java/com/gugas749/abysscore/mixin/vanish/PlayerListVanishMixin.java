package com.gugas749.abysscore.mixin.vanish;

import com.gugas749.abysscore.features.vanish.VanishHooks;
import com.gugas749.abysscore.features.vanish.VanishMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.function.Function;

@Mixin(PlayerList.class)
public abstract class PlayerListVanishMixin {

    // ── Join message: the joining player is the "subject" while placeNewPlayer runs ──
    //
    // placeNewPlayer's parameters differ between versions (1.21.1 has an extra
    // CommonListenerCookie, 1.20.1 doesn't), and an @Inject handler must list ALL of them.
    // So we grab the player with @ModifyVariable instead: argsOnly = true + the type
    // ServerPlayer finds "the ServerPlayer parameter" in both versions. We return it
    // unchanged; we only wanted to see it. The RETURN handler takes only CallbackInfo
    // (allowed when you list no parameters at all).

    @ModifyVariable(method = "placeNewPlayer", at = @At("HEAD"), argsOnly = true)
    private ServerPlayer abysscore$joinStart(ServerPlayer player) {
        VanishMessages.push(player);
        return player;
    }

    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void abysscore$joinEnd(CallbackInfo ci) {
        VanishMessages.pop();
    }

    // ── Every system broadcast (join/leave/death/advancement) passes here ──
    // The 2-argument broadcastSystemMessage(Component, boolean) calls this one, so one hook covers both.

    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Ljava/util/function/Function;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void abysscore$silentMessages(Component message, Function<ServerPlayer, Component> perPlayer,
                                          boolean bypassHiddenChat, CallbackInfo ci) {
        if (VanishMessages.handleBroadcast((PlayerList) (Object) this, message, perPlayer, bypassHiddenChat)) {
            ci.cancel();
        }
    }

    @Inject(method = "broadcastSystemToTeam", at = @At("HEAD"), cancellable = true)
    private void abysscore$silentTeamMessage(Player player, Component message, CallbackInfo ci) {
        if (VanishMessages.handleTeamBroadcast((PlayerList) (Object) this, player, message, true)) {
            ci.cancel();
        }
    }

    @Inject(method = "broadcastSystemToAllExceptTeam", at = @At("HEAD"), cancellable = true)
    private void abysscore$silentNonTeamMessage(Player player, Component message, CallbackInfo ci) {
        if (VanishMessages.handleTeamBroadcast((PlayerList) (Object) this, player, message, false)) {
            ci.cancel();
        }
    }

    // ── Sounds and block effects caused by a player (steps, breaking, placing, eating...) ──

    @Inject(method = "broadcast", at = @At("HEAD"), cancellable = true)
    private void abysscore$silentSounds(@Nullable Player except, double x, double y, double z, double radius,
                                        ResourceKey<Level> dimension, Packet<?> packet, CallbackInfo ci) {
        if (VanishHooks.broadcastNear((PlayerList) (Object) this, except, x, y, z, radius, dimension, packet)) {
            ci.cancel();
        }
    }
}
