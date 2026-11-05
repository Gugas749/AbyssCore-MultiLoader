package com.gugas749.abysscore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import com.gugas749.abysscore.platform.Services;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class AbysscoreServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE =
            Services.PLATFORM.getConfigDir().resolve("abysscore/abysscore-server.json");

    // ── Values with defaults ──────────────────────────────────────────────────
    private static boolean chatLockEnabled    = true;
    private static boolean helpWebhookEnabled = false;
    private static String  helpWebhookUrl     = "";
    // Who can see vanished players: PLAYER, MODERATOR or ADMIN (ADMIN = op level 2)
    private static AbyssPermissionLevel vanishSeeLevel = AbyssPermissionLevel.ADMIN;

    // ── Getters ───────────────────────────────────────────────────────────────
    public static boolean isChatLockEnabled()    { return chatLockEnabled; }
    public static boolean isHelpWebhookEnabled() { return helpWebhookEnabled; }
    public static String  getHelpWebhookUrl()    { return helpWebhookUrl; }
    public static AbyssPermissionLevel getVanishSeeLevel() { return vanishSeeLevel; }

    // ── Load / Save ───────────────────────────────────────────────────────────

    public static void load() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
        } catch (IOException e) {
            Constants.LOG.error("[AbyssCore] Failed to create config directory: {}", e.getMessage());
        }

        if (!Files.exists(CONFIG_FILE)) {
            save(); // write defaults on first run
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            if (obj == null) return;

            if (obj.has("chat_lock_enabled"))
                chatLockEnabled = obj.get("chat_lock_enabled").getAsBoolean();
            if (obj.has("help_webhook_enabled"))
                helpWebhookEnabled = obj.get("help_webhook_enabled").getAsBoolean();
            if (obj.has("help_webhook_url"))
                helpWebhookUrl = obj.get("help_webhook_url").getAsString();
            if (obj.has("vanish_see_level")) {
                try {
                    vanishSeeLevel = AbyssPermissionLevel.valueOf(obj.get("vanish_see_level").getAsString().toUpperCase());
                } catch (IllegalArgumentException e) {
                    Constants.LOG.warn("[AbyssCore] vanish_see_level must be PLAYER, MODERATOR or ADMIN. Using ADMIN.");
                    vanishSeeLevel = AbyssPermissionLevel.ADMIN;
                }
            } else {
                save();   // old config file: write the new key with its default
            }

            Constants.LOG.info("[AbyssCore] Server config loaded.");
        } catch (IOException e) {
            Constants.LOG.error("[AbyssCore] Failed to load server config: {}", e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
        } catch (IOException e) {
            Constants.LOG.error("[AbyssCore] Failed to create config directory: {}", e.getMessage());
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("chat_lock_enabled", chatLockEnabled);
        obj.addProperty("help_webhook_enabled", helpWebhookEnabled);
        obj.addProperty("help_webhook_url", helpWebhookUrl);
        obj.addProperty("vanish_see_level", vanishSeeLevel.name());

        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(obj, writer);
        } catch (IOException e) {
            Constants.LOG.error("[AbyssCore] Failed to save server config: {}", e.getMessage());
        }
    }
}
