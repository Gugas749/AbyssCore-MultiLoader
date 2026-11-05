package com.gugas749.abysscore.features.vanish;

import net.minecraft.server.level.ServerPlayer;

/**
 * Plain event methods for vanish. The loader listeners (NeoForgeEventListeners /
 * ForgeEventListeners) call these; all the logic is in VanishManager.
 *
 * Before: this class polled Vanishmod's VANISHED_PLAYERS field with reflection every
 * second. Now AbyssCore owns the vanish state, so it sends the HUD packet the moment the
 * state changes instead of polling.
 */
public class ACVanishStateListener {

    public void onPlayerJoin(ServerPlayer player) {
        VanishManager.onJoin(player);
    }

    public void onPlayerLeave(ServerPlayer player) {
        VanishManager.onLeave(player);
        // ACVanishExtras.onPlayerLeave(uuid) is called separately by the loader listeners
    }

    public void onPlayerTick(ServerPlayer player) {
        VanishMessages.reset();   // safety net, see VanishMessages
        VanishManager.onTick(player);
    }

    /** Returns true if the chat message must be cancelled (BLOCK_CHAT option). */
    public boolean onChat(ServerPlayer player) {
        return VanishManager.onChat(player);
    }

    /** Kept for old callers: is this player vanished (AbyssCore's own vanish now). */
    public static boolean isVanished(ServerPlayer player) {
        return VanishManager.isVanished(player);
    }
}
