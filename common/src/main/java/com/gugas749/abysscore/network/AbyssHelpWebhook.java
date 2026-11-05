package com.gugas749.abysscore.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.gugas749.abysscore.AbysscoreServerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class AbyssHelpWebhook {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void sendDCMessage(String playerName, String playerUUID, String reason) {
        if (!AbysscoreServerConfig.isHelpWebhookEnabled()) return;
        String webhookUrl = AbysscoreServerConfig.getHelpWebhookUrl();
        if (webhookUrl.isBlank()) {
            LOGGER.warn("[Abysshelp] Discord webhook URL not configured, skipping log.");
            return;
        }

        // A background thread so the HTTP call never freezes the server.
        // (Was Thread.ofVirtual(): Java 21 only, and Forge 1.20.1 runs on Java 17.)
        Thread thread = new Thread(() -> {
            try {
                // Build JSON safely with Gson
                JsonObject field1 = new JsonObject();
                field1.addProperty("name", "Player");
                field1.addProperty("value", "`" + playerName + "`");
                field1.addProperty("inline", true);

                JsonObject field2 = new JsonObject();
                field2.addProperty("name", "UUID");
                field2.addProperty("value", "`" + playerUUID + "`");
                field2.addProperty("inline", true);

                JsonObject field3 = new JsonObject();
                field3.addProperty("name", "Reason");
                field3.addProperty("value", reason);
                field3.addProperty("inline", false);

                JsonArray fields = new JsonArray();
                fields.add(field1); fields.add(field2); fields.add(field3);

                JsonObject footer = new JsonObject();
                footer.addProperty("text", "AbyssCore");

                JsonObject embed = new JsonObject();
                embed.addProperty("title", "Pedido de ajuda - AbyssHelp");
                embed.addProperty("color", 0x0000FF);
                embed.add("fields", fields);
                embed.add("footer", footer);
                embed.addProperty("timestamp", Instant.now().toString());

                JsonArray embeds = new JsonArray();
                embeds.add(embed);

                JsonObject payload = new JsonObject();
                payload.add("embeds", embeds);

                String json = new Gson().toJson(payload);

                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    LOGGER.info("[Abysshelp] Discord webhook sent for player '{}'.", playerName);
                } else {
                    LOGGER.warn("[Abysshelp] Discord webhook returned status: {}", responseCode);
                }
                connection.disconnect();
            } catch (Exception e) {
                LOGGER.warn("[Abysshelp] Failed to send Discord webhook: {}", e.getMessage());
            }
        }, "AbyssCore-HelpWebhook");
        thread.setDaemon(true);   // never keeps the server process alive
        thread.start();
    }
}
