package com.gugas749.abysscore.features.chat;

import com.gugas749.abysscore.AbysscoreServerConfig;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Blocks chat and specific commands when chat lock is enabled.
 * Event wiring (NeoForge) lives in NeoForgeEventListeners; only plain methods here.
 */
public class ACChatLockListener {

    private static final Set<String> BLOCKED_COMMANDS = Set.of("say", "me");
    private static final Set<UUID> mutedAdmins = new HashSet<>();

    public static void toggleAdminMute(UUID uuid) {
        if (mutedAdmins.contains(uuid)) mutedAdmins.remove(uuid);
        else mutedAdmins.add(uuid);
    }

    public static boolean isAdminMuted(UUID uuid) {
        return mutedAdmins.contains(uuid);
    }

    /** Returns true if the chat message should be cancelled. */
    public boolean onChat(ServerPlayer player) {
        if (AbyssPermissionHandler.has(player, AbyssPermissionLevel.MODERATOR)) {
            return isAdminMuted(player.getUUID());
        }
        return AbysscoreServerConfig.isChatLockEnabled();
    }

    /**
     * Returns true if the command should be cancelled.
     * commandInput should be the raw command string (with or without leading slash).
     */
    public boolean onCommand(ServerPlayer player, String commandInput) {
        if (player.hasPermissions(2)) return false;
        String input = commandInput.trim();
        if (input.startsWith("/")) input = input.substring(1);
        String commandName = input.split(" ")[0].toLowerCase();
        return BLOCKED_COMMANDS.contains(commandName) && AbysscoreServerConfig.isChatLockEnabled();
    }
}
