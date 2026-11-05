package com.gugas749.abysscore.api.attachment;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.network.PacketHandler;
import com.gugas749.abysscore.platform.Services;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-player data that is saved with the player AND synced to that player's client.
 * Same API on NeoForge 1.21.1 and Forge 1.20.1 (before, each branch had its own version).
 *
 *   // in your mod, as a static field (so it's created while mods are being constructed):
 *   public static final AbyssSyncedAttachment<Integer> MANA =
 *           AbyssSyncedAttachment.register("mymod", "mana", 0, Codec.INT);
 *
 *   MANA.get(player);            // server: the saved value — client: the last value the server synced
 *   MANA.set(serverPlayer, 50);  // server: save + send to that player's client
 *
 * How it works:
 *   - Saving is the loader's job (AttachmentStorage, from IAttachmentPlatform).
 *   - Syncing is ours: set() sends a SyncAttachmentPacket; the client keeps the value in
 *     {@link #clientValue}. The client never reads the loader storage (it isn't synced there).
 *   - Copied to the new player object on respawn / End return, and re-sent on login, respawn and
 *     dimension change — the loader listeners call the on...() methods below.
 *
 * Values must be IMMUTABLE (records, Integer, String...): changing an object in place would skip set()
 * and the client would never hear about it.
 */
public final class AbyssSyncedAttachment<T> {

    private static final Map<ResourceLocation, AbyssSyncedAttachment<?>> REGISTRY = new LinkedHashMap<>();

    private final ResourceLocation id;
    private final T defaultValue;
    private final Codec<T> codec;
    private final AttachmentStorage<T> storage;

    /** Only written on the client (SyncAttachmentClientHandler). volatile: read by the render thread. */
    private volatile T clientValue;

    private AbyssSyncedAttachment(ResourceLocation id, T defaultValue, Codec<T> codec, AttachmentStorage<T> storage) {
        this.id = id;
        this.defaultValue = defaultValue;
        this.codec = codec;
        this.storage = storage;
        this.clientValue = defaultValue;
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /** Registers under YOUR mod's namespace (modId:name). Call while your mod is being constructed. */
    public static <T> AbyssSyncedAttachment<T> register(String modId, String name, T defaultValue, Codec<T> codec) {
        ResourceLocation id = Constants.id(modId, name);
        if (REGISTRY.containsKey(id)) {
            throw new IllegalStateException("Duplicate AbyssSyncedAttachment id: " + id);
        }
        AttachmentStorage<T> storage = Services.ATTACHMENTS.createStorage(id, defaultValue, codec);
        AbyssSyncedAttachment<T> attachment = new AbyssSyncedAttachment<>(id, defaultValue, codec, storage);
        REGISTRY.put(id, attachment);
        return attachment;
    }

    /** Registers under the abysscore namespace (kept so old callers still compile). */
    public static <T> AbyssSyncedAttachment<T> register(String name, T defaultValue, Codec<T> codec) {
        return register(Constants.MOD_ID, name, defaultValue, codec);
    }

    public static Map<ResourceLocation, AbyssSyncedAttachment<?>> all() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    public static AbyssSyncedAttachment<?> byId(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    public ResourceLocation id() {
        return id;
    }

    public T defaultValue() {
        return defaultValue;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Server player → the saved value. Client player → the last value the server synced
     * (only YOUR own player's values are synced, other players' read as the default).
     */
    public T get(Player player) {
        if (player.level().isClientSide()) {
            // isLocalPlayer(): true only for the client's own LocalPlayer (vanilla, both versions).
            // Without this check, another player's entity on the client returned OUR value.
            return player.isLocalPlayer() ? clientValue : defaultValue;
        }
        return storage.get(player);
    }

    /**
     * Same as get(Player). Kept because the old versions had get(ServerPlayer): mods compiled
     * against them call exactly this method, and without it they'd crash (NoSuchMethodError).
     */
    public T get(ServerPlayer player) {
        return get((Player) player);
    }

    /** Client: the last value the server synced to this client. */
    public T getClient() {
        return clientValue;
    }

    // ── Write (server) ────────────────────────────────────────────────────────

    /** Saves the value and sends it to that player's client. */
    public void set(ServerPlayer player, T value) {
        storage.set(player, value);
        sync(player);
    }

    /**
     * Saves WITHOUT telling the client — for values that change every tick when the client only
     * needs some of the updates. Call sync(player) when it matters.
     */
    public void setSilently(ServerPlayer player, T value) {
        storage.set(player, value);
    }

    /** Sends the current saved value to that player's client. */
    public void sync(ServerPlayer player) {
        PacketHandler.CHANNEL.sendToPlayer(player, new SyncAttachmentPacket(id, AttachmentNbt.encode(codec, get(player), id)));
    }

    public static void syncAll(ServerPlayer player) {
        for (AbyssSyncedAttachment<?> attachment : REGISTRY.values()) attachment.sync(player);
    }

    // ── Client side ───────────────────────────────────────────────────────────

    /** Called by SyncAttachmentClientHandler with the packet's data. */
    void applyClientValue(net.minecraft.nbt.CompoundTag data) {
        this.clientValue = AttachmentNbt.decode(codec, data, defaultValue, id);
    }

    //-----------------------------------------------------------------------------------
    //                         EVENTS (called from the loader listeners)
    //-----------------------------------------------------------------------------------

    /**
     * Respawn and End return create a NEW player object (PlayerEvent.Clone): copy every value
     * from the old object, so nothing is lost on death.
     */
    public static void onPlayerClone(ServerPlayer original, ServerPlayer clone) {
        for (AbyssSyncedAttachment<?> attachment : REGISTRY.values()) attachment.copy(original, clone);
    }

    private void copy(Player original, Player clone) {
        storage.set(clone, storage.get(original));
    }

    /**
     * Login: the client still holds whatever the LAST server sent (or the default).
     * Respawn / dimension change: the data was just copied to a new player object.
     * In all three cases, send everything again.
     */
    public static void onLogin(ServerPlayer player)           { syncAll(player); }
    public static void onRespawn(ServerPlayer player)         { syncAll(player); }
    public static void onChangeDimension(ServerPlayer player) { syncAll(player); }

    /** Client left a world/server: forget the values the old server sent. */
    public static void onClientLogout() {
        for (AbyssSyncedAttachment<?> attachment : REGISTRY.values()) attachment.resetClient();
    }

    private void resetClient() {
        this.clientValue = defaultValue;
    }
}
