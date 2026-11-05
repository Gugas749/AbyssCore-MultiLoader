package com.gugas749.abysscore.features.vanish.compat;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.features.vanish.VanishManager;
import com.gugas749.abysscore.features.vanish.VanishOption;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.events.StaticSoundPacketEvent;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

/**
 * Simple Voice Chat compat (option MUTE_VOICE): players who can't see you can't hear you.
 *
 * How Simple Voice Chat finds this class: it scans every mod jar for @ForgeVoicechatPlugin
 * (on Forge AND NeoForge). The annotation is in voicechat-api, not in a loader, so this
 * class lives in common. If Simple Voice Chat isn't installed nobody loads this class, so
 * AbyssCore works fine without it (the API is only a compileOnly dependency).
 *
 * How it works: for every voice packet the server is about to send, SVC fires an event with
 * the sender and the receiver. We cancel it when the receiver can't see the sender.
 * Group chat is NOT blocked: joining a group with someone is a choice you make.
 */
@ForgeVoicechatPlugin
public class VanishVoiceChatPlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return Constants.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        Constants.LOG.info("[AbyssCore] Simple Voice Chat found: vanished players are muted for who can't see them.");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(EntitySoundPacketEvent.class, this::onSound);      // proximity, attached to you
        registration.registerEvent(LocationalSoundPacketEvent.class, this::onSound);  // proximity, at a position
        registration.registerEvent(StaticSoundPacketEvent.class, this::onSound);      // groups, spectator, plugins
    }

    private void onSound(SoundPacketEvent<?> event) {
        if (SoundPacketEvent.SOURCE_GROUP.equals(event.getSource())) return;

        ServerPlayer sender = player(event.getSenderConnection());
        ServerPlayer receiver = player(event.getReceiverConnection());
        if (sender == null || receiver == null) return;

        if (VanishManager.isActive(sender, VanishOption.MUTE_VOICE) && !VanishManager.canSee(receiver, sender)) {
            event.cancel();
        }
    }

    /** SVC wraps the Minecraft player; getPlayer() gives back the real ServerPlayer. */
    @Nullable
    private static ServerPlayer player(@Nullable VoicechatConnection connection) {
        if (connection == null) return null;
        Object player = connection.getPlayer().getPlayer();
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }
}
