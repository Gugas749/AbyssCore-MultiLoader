package com.gugas749.abysscore.features.vanish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gugas749.abysscore.AbysscoreServerConfig;
import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.network.AbyssNetwork;
import com.gugas749.abysscore.network.vanish.VanishStateSyncPacket;
import com.gugas749.abysscore.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbyssCore's own vanish. Holds who is vanished, each player's options, and the ONE rule
 * that decides who can see whom: {@link #canSee}.
 *
 * How hiding works (every piece asks canSee):
 *   - body        → ServerPlayerVanishMixin: vanilla asks broadcastToPlayer() before it sends
 *                   an entity to a client; we answer false.
 *   - tab list    → ServerCommonPacketListenerVanishMixin: player-info packets are filtered
 *                   per receiver.
 *   - messages    → VanishMessages + mixins (join/leave/death/advancement).
 *   - the rest    → small mixins, each behind a VanishOption (pickup, mobs, sounds, ...).
 *
 * Saved in config/abysscore/abysscore_vanish.json so vanish survives relogs and restarts.
 * Everything here runs on the server thread.
 */
public final class VanishManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SAVE_PATH =
            Services.PLATFORM.getConfigDir().resolve("abysscore/abysscore_vanish.json");

    /*
     * Concurrent collections because Simple Voice Chat calls canSee() from ITS OWN thread
     * (VanishVoiceChatPlugin) while the server thread may be changing these. A plain
     * HashSet read during a write can throw or give garbage; these can't.
     */

    /** Players who are vanished (online or not). */
    private static final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    /** Each player's options. Missing player or missing option = the option's default. */
    private static final Map<UUID, EnumSet<VanishOption>> options = new ConcurrentHashMap<>();

    /**
     * For each online vanished player: which viewers we last told "you can see them".
     * The tick check compares this with canSee() so a change (op/deop, team change,
     * new player) is applied within a second, without spamming packets every tick.
     */
    private static final Map<UUID, Set<UUID>> lastVisibleTo = new HashMap<>();

    private VanishManager() {}

    //-----------------------------------------------------------------------------------
    //                                    QUERIES
    //-----------------------------------------------------------------------------------

    public static boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    /** True only for a SERVER player that is vanished (client entities are never "vanished"). */
    public static boolean isVanished(@Nullable Entity entity) {
        return entity instanceof ServerPlayer && vanished.contains(entity.getUUID());
    }

    /** True if nobody is vanished. Mixins check this first so they cost nothing normally. */
    public static boolean nobodyVanished() {
        return vanished.isEmpty();
    }

    public static Set<UUID> getVanished() {
        return Collections.unmodifiableSet(vanished);
    }

    /** The option's value for this player, vanished or not. */
    public static boolean getOption(UUID uuid, VanishOption option) {
        EnumSet<VanishOption> set = options.get(uuid);
        return set == null ? option.defaultValue : set.contains(option);
    }

    /** All options of a player packed into an int (one bit each) for the GUI packet. */
    public static int getOptionBits(UUID uuid) {
        int bits = 0;
        for (VanishOption option : VanishOption.values()) {
            if (getOption(uuid, option)) bits |= option.bit();
        }
        return bits;
    }

    /**
     * The check most mixins use: is this entity a vanished player WITH this option on?
     * e.g. isActive(this, VanishOption.NO_PICKUP) in the item pickup mixin.
     */
    public static boolean isActive(@Nullable Entity entity, VanishOption option) {
        return isVanished(entity) && getOption(entity.getUUID(), option);
    }

    /**
     * THE visibility rule. Can {@code viewer} see {@code target}?
     *
     * In order:
     *   1. target not vanished          → yes
     *   2. viewer is the target         → yes (you always see yourself)
     *   3. per-player exception         → whatever /abysscore vanish show|hide said
     *   4. viewer has the "see" level   → yes (config: vanish_see_level, default ADMIN)
     *   5. team visibility ON + same team → yes
     *   6. otherwise                    → no
     */
    public static boolean canSee(@Nullable Entity viewer, Entity target) {
        if (!isVanished(target)) return true;
        if (!(viewer instanceof ServerPlayer viewerPlayer)) return true;   // console, command blocks...
        if (viewer == target) return true;

        Boolean exception = ACVanishExtras.checkException(target.getUUID(), viewer.getUUID());
        if (exception != null) return exception;

        if (AbyssPermissionHandler.has(viewerPlayer, AbysscoreServerConfig.getVanishSeeLevel())) return true;

        return ACVanishExtras.isTeamVisibilityEnabled()
                && target.getTeam() != null
                && target.getTeam() == viewer.getTeam();
    }

    /** Online players that can NOT see target. */
    public static List<ServerPlayer> hiddenFrom(ServerPlayer target) {
        List<ServerPlayer> list = new ArrayList<>();
        for (ServerPlayer viewer : target.getServer().getPlayerList().getPlayers()) {
            if (!canSee(viewer, target)) list.add(viewer);
        }
        return list;
    }

    //-----------------------------------------------------------------------------------
    //                                    CHANGES
    //-----------------------------------------------------------------------------------

    public static boolean toggle(ServerPlayer player) {
        setVanished(player, !isVanished(player.getUUID()));
        return isVanished(player.getUUID());
    }

    public static void setVanished(ServerPlayer player, boolean vanish) {
        UUID uuid = player.getUUID();
        if (vanish == vanished.contains(uuid)) return;

        if (vanish) {
            vanished.add(uuid);
            // AFTER adding, so hiddenFrom() returns the players who just lost sight of them
            if (getOption(uuid, VanishOption.FAKE_MESSAGES)) {
                sendFake(player, "multiplayer.player.left", hiddenFrom(player));
            }
        } else {
            // BEFORE removing, for the same reason the other way around
            List<ServerPlayer> couldNotSee = hiddenFrom(player);
            vanished.remove(uuid);
            lastVisibleTo.remove(uuid);
            if (getOption(uuid, VanishOption.FAKE_MESSAGES)) {
                sendFake(player, "multiplayer.player.joined", couldNotSee);
            }
        }
        save();

        refresh(player);
        applyNightVision(player);
        syncClient(player);

        if (getOption(uuid, VanishOption.CHAT_NOTICE)) {
            player.sendSystemMessage(Component.translatable(
                    vanish ? "message.abysscore.vanish.now_vanished" : "message.abysscore.vanish.now_visible"));
        }
        Constants.LOG.info("[AbyssCore] {} is now {}.", player.getName().getString(), vanish ? "vanished" : "visible");
    }

    public static void setOption(ServerPlayer player, VanishOption option, boolean value) {
        EnumSet<VanishOption> set = options.computeIfAbsent(player.getUUID(), uuid -> defaults());
        if (value) set.add(option);
        else set.remove(option);
        save();

        // Options whose effect has to be applied right away
        switch (option) {
            case HUD_WARNING -> syncClient(player);
            case NIGHT_VISION -> applyNightVision(player);
            default -> { }
        }
    }

    private static EnumSet<VanishOption> defaults() {
        EnumSet<VanishOption> set = EnumSet.noneOf(VanishOption.class);
        for (VanishOption option : VanishOption.values()) {
            if (option.defaultValue) set.add(option);
        }
        return set;
    }

    //-----------------------------------------------------------------------------------
    //                                    REFRESH
    //-----------------------------------------------------------------------------------

    /**
     * Makes every client match canSee() for {@code target} RIGHT NOW. Call after anything
     * that changes visibility (toggle, show/hide exception, team visibility).
     *
     * Order matters: a client can only spawn a player entity it has in its tab list
     * (player info). So: 1) add info for whoever can see, 2) re-run entity tracking,
     * 3) remove info for whoever can't (after the entity is already gone).
     */
    public static void refresh(ServerPlayer target) {
        MinecraftServer server = target.getServer();
        if (server == null) return;

        List<ServerPlayer> canSeeList = new ArrayList<>();
        List<ServerPlayer> cannotSeeList = new ArrayList<>();
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (viewer == target) continue;
            (canSee(viewer, target) ? canSeeList : cannotSeeList).add(viewer);
        }

        // 1. tab list entry for the ones who can see (a client ignores a duplicate add)
        if (!canSeeList.isEmpty()) {
            var addInfo = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(target));
            for (ServerPlayer viewer : canSeeList) viewer.connection.send(addInfo);
        }

        // 2. entity tracking: ChunkMap.move() re-asks broadcastToPlayer() for every player
        //    (our mixin answers it) and spawns / despawns the body to match.
        target.serverLevel().getChunkSource().move(target);

        // 3. tab list entry gone for the ones who can't see
        if (!cannotSeeList.isEmpty()) {
            var removeInfo = new ClientboundPlayerInfoRemovePacket(List.of(target.getUUID()));
            for (ServerPlayer viewer : cannotSeeList) viewer.connection.send(removeInfo);
        }

        if (isVanished(target)) {
            Set<UUID> seen = new HashSet<>();
            for (ServerPlayer viewer : canSeeList) seen.add(viewer.getUUID());
            lastVisibleTo.put(target.getUUID(), seen);
        }
    }

    /** refresh() for every online vanished player (team visibility toggled, config reload...). */
    public static void refreshAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isVanished(player)) refresh(player);
        }
    }

    /** Tells the player's client whether to draw the HUD warning. */
    public static void syncClient(ServerPlayer player) {
        boolean isVanished = isVanished(player);
        AbyssNetwork.sendToPlayer(player, new VanishStateSyncPacket(
                isVanished, isVanished && getOption(player.getUUID(), VanishOption.HUD_WARNING)));
    }

    private static void applyNightVision(ServerPlayer player) {
        boolean want = isActive(player, VanishOption.NIGHT_VISION);
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        // "Ours" = infinite + no particles. We never remove a night vision potion the player drank.
        boolean ours = current != null && current.isInfiniteDuration() && !current.isVisible();

        if (want && current == null) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
                    MobEffectInstance.INFINITE_DURATION, 0, false, false));
        } else if (!want && ours) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    private static void sendFake(ServerPlayer player, String langKey, List<ServerPlayer> receivers) {
        Component message = Component.translatable(langKey, player.getDisplayName()).withStyle(ChatFormatting.YELLOW);
        for (ServerPlayer receiver : receivers) receiver.sendSystemMessage(message);
    }

    //-----------------------------------------------------------------------------------
    //                        EVENTS (called from ACVanishStateListener)
    //-----------------------------------------------------------------------------------

    static void onJoin(ServerPlayer player) {
        syncClient(player);
        if (!isVanished(player)) return;

        // The tab list / body of the joiner are already hidden by the packet filter and
        // broadcastToPlayer, because the vanished set was loaded at server start.
        applyNightVision(player);
        if (getOption(player.getUUID(), VanishOption.CHAT_NOTICE)) {
            player.sendSystemMessage(Component.translatable("message.abysscore.vanish.still_vanished"));
        }
    }

    static void onLeave(ServerPlayer player) {
        lastVisibleTo.remove(player.getUUID());
        // A viewer who left may be in someone's lastVisibleTo; the tick check fixes that.
    }

    /** Once per second per vanished player: re-apply night vision, fix visibility drift. */
    static void onTick(ServerPlayer player) {
        if (player.tickCount % 20 != 0 || !isVanished(player)) return;

        applyNightVision(player);   // death / milk / another mod removed it

        Set<UUID> now = new HashSet<>();
        for (ServerPlayer viewer : player.getServer().getPlayerList().getPlayers()) {
            if (viewer != player && canSee(viewer, player)) now.add(viewer.getUUID());
        }
        if (!now.equals(lastVisibleTo.get(player.getUUID()))) {
            refresh(player);
        }
    }

    /** Returns true if the chat message must be cancelled. */
    static boolean onChat(ServerPlayer player) {
        if (!isActive(player, VanishOption.BLOCK_CHAT)) return false;
        player.sendSystemMessage(Component.translatable("message.abysscore.vanish.chat_blocked"));
        return true;
    }

    //-----------------------------------------------------------------------------------
    //                                  LOAD / SAVE
    //-----------------------------------------------------------------------------------

    /*
     * File format:
     * {
     *   "team_visibility": true,
     *   "vanished": [ "uuid", ... ],
     *   "options": { "uuid": { "hud_warning": true, "night_vision": false, ... } }
     * }
     */

    public static void load() {
        vanished.clear();
        options.clear();
        lastVisibleTo.clear();

        if (!Files.exists(SAVE_PATH)) return;

        try (Reader reader = Files.newBufferedReader(SAVE_PATH)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) return;

            if (root.has("team_visibility")) {
                ACVanishExtras.setTeamVisibility(root.get("team_visibility").getAsBoolean());
            }
            if (root.has("vanished")) {
                for (JsonElement e : root.getAsJsonArray("vanished")) {
                    vanished.add(UUID.fromString(e.getAsString()));
                }
            }
            if (root.has("options")) {
                for (Map.Entry<String, JsonElement> player : root.getAsJsonObject("options").entrySet()) {
                    EnumSet<VanishOption> set = defaults();
                    for (Map.Entry<String, JsonElement> opt : player.getValue().getAsJsonObject().entrySet()) {
                        VanishOption option = VanishOption.byId(opt.getKey());
                        if (option == null) continue;   // option removed in a newer version
                        if (opt.getValue().getAsBoolean()) set.add(option);
                        else set.remove(option);
                    }
                    options.put(UUID.fromString(player.getKey()), set);
                }
            }
            Constants.LOG.info("[AbyssCore] Vanish loaded: {} vanished player(s).", vanished.size());
        } catch (IOException | RuntimeException e) {
            // RuntimeException: bad JSON or bad UUID — keep the server running
            Constants.LOG.error("[AbyssCore] Failed to load vanish data: {}", e.getMessage());
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("team_visibility", ACVanishExtras.isTeamVisibilityEnabled());

        JsonArray list = new JsonArray();
        vanished.forEach(uuid -> list.add(uuid.toString()));
        root.add("vanished", list);

        JsonObject allOptions = new JsonObject();
        options.forEach((uuid, set) -> {
            JsonObject obj = new JsonObject();
            for (VanishOption option : VanishOption.values()) obj.addProperty(option.id(), set.contains(option));
            allOptions.add(uuid.toString(), obj);
        });
        root.add("options", allOptions);

        try {
            Files.createDirectories(SAVE_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(SAVE_PATH)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            Constants.LOG.error("[AbyssCore] Failed to save vanish data: {}", e.getMessage());
        }
    }
}
