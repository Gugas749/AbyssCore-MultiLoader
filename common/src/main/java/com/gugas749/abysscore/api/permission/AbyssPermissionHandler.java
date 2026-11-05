package com.gugas749.abysscore.api.permission;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.platform.Services;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class AbyssPermissionHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE =
            Services.PLATFORM.getConfigDir().resolve("abysscore/abysscore_permissions.json");

    private static final Map<UUID, AbyssPermissionLevel> permissions = new HashMap<>();

    public static void grant(UUID uuid, AbyssPermissionLevel level) {
        permissions.put(uuid, level);
        save();
    }
    public static void revoke(UUID uuid) {
        permissions.put(uuid, AbyssPermissionLevel.PLAYER);
        save();
    }
    public static boolean has(ServerPlayer player, AbyssPermissionLevel required) {
        if (required == AbyssPermissionLevel.ADMIN) {
            return player.hasPermissions(2);
        }

        AbyssPermissionLevel level = permissions.getOrDefault(
                player.getUUID(), AbyssPermissionLevel.PLAYER);
        return level.isAtLeast(required);
    }

    /**
     * For command .requires(...). A player is checked with has(...).
     * Anything else (server console, command blocks, functions, /execute without "as <player>")
     * counts as ADMIN when it has vanilla permission level 2 — the same bar a player needs for
     * ADMIN — so staff commands also work from the console. Commands that need a PLAYER must
     * still check source.getEntity() themselves.
     */
    public static boolean sourceHas(CommandSourceStack source, AbyssPermissionLevel required) {
        if (source.getEntity() instanceof ServerPlayer player) return has(player, required);
        return source.hasPermission(2);
    }

    /**
     * For packet handlers: checks the level and, if the player is missing it, tells them
     * instead of silently ignoring the click.
     *
     *   if (!AbyssPermissionHandler.require(player, AbyssPermissionLevel.MODERATOR)) return;
     */
    public static boolean require(ServerPlayer player, AbyssPermissionLevel required) {
        if (has(player, required)) return true;
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message.abysscore.no_permission", required.name()));
        return false;
    }

    // ── Load / Save ───────────────────────────────────────────────────────────

    public static void load() {
        permissions.clear();

        try {
            Files.createDirectories(CONFIG_FILE.getParent());
        } catch (IOException e) {
            Constants.LOG.error("[AbyssCore] Failed to create config directory: {}", e.getMessage());
        }

        if (!Files.exists(CONFIG_FILE)) return;
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            // Use JsonObject not JsonArray — it's a key/value map
            var obj = GSON.fromJson(reader, com.google.gson.JsonObject.class);
            if (obj != null) obj.entrySet().forEach(e -> {
                UUID uuid = UUID.fromString(e.getKey());
                AbyssPermissionLevel level = AbyssPermissionLevel.valueOf(
                        e.getValue().getAsString()); // "MODERATOR" → enum constant
                permissions.put(uuid, level);
            });
            Constants.LOG.info("[AbyssCore] Loaded {} permission(s).", permissions.size());
        } catch (IOException e) {
            Constants.LOG.error("[AbyssCore] Failed to load permissions: {}", e.getMessage());
        }
    }

    public static void save() {
        var obj = new com.google.gson.JsonObject();
        // UUID.toString() → "MODERATOR" string
        permissions.forEach((uuid, level) -> obj.addProperty(uuid.toString(), level.name()));
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(obj, writer);
        } catch (IOException e) {
            Constants.LOG.error("[AbyssCore] Failed to save permissions: {}", e.getMessage());
        }
    }

    // ── INIT OP PLAYERS ──────────────────────────────────────────────────────
    public static void onPlayerJoin(ServerPlayer player) {
        UUID uuid = player.getUUID();

        if (player.hasPermissions(2)) {
            // OP — grant ADMIN if not already in map
            if (!permissions.containsKey(uuid)) {
                permissions.put(uuid, AbyssPermissionLevel.ADMIN);
                save();
                Constants.LOG.info("[AbyssCore] Auto-granted ADMIN to OP player {}", player.getName().getString());
            }
        } else {
            // Not OP — if they somehow have ADMIN in the map, strip it down to PLAYER
            if (permissions.getOrDefault(uuid, AbyssPermissionLevel.PLAYER) == AbyssPermissionLevel.ADMIN) {
                permissions.put(uuid, AbyssPermissionLevel.PLAYER);
                save();
                Constants.LOG.info("[AbyssCore] Stripped ADMIN from non-OP player {}", player.getName().getString());
            }
        }
    }
}
