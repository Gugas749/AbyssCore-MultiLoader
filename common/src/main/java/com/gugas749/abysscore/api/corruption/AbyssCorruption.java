package com.gugas749.abysscore.api.corruption;

import com.gugas749.abysscore.api.attachment.AbyssSyncedAttachment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * ABY-16 — the shared corruption system.
 *
 * Core owns the MECHANISM: a 0–100 level per player, saved with the player, kept on death,
 * synced to the client and shown as a HUD bar. Mods own the POLICY: what raises it and what
 * it does (AbyssItems: Void Essence gain + strength/dodge; AbyssOrigins: skills that inflict it).
 *
 *   AbyssCorruption.add(target, 5);
 *   AbyssCorruption.addProgress(player, 86_400);
 *   int level = AbyssCorruption.get(player);
 *
 * Server side only, except get(Player) which also works on the client (for HUDs).
 */
public final class AbyssCorruption {

    public static final int MAX = 100;

    /** Registered as "abysscore:corruption". Synced to the owning player's client. */
    public static final AbyssSyncedAttachment<CorruptionData> ATTACHMENT =
            AbyssSyncedAttachment.register("corruption", CorruptionData.NONE, CorruptionData.CODEC);

    private AbyssCorruption() {}

    /**
     * Calling this loads the class, which creates the ATTACHMENT static field (registering it).
     * Called from each loader's mod constructor: attachments must be registered while mods
     * are being constructed (NeoForge registers its attachment types in a registry event).
     */
    public static void init() {}

    // ── Read ──────────────────────────────────────────────────────────────────

    /** Works on both sides: the server's saved value, or on the client the last synced value. */
    public static int get(Player player) {
        return data(player).level();
    }

    public static CorruptionData data(Player player) {
        return ATTACHMENT.get(player);
    }

    // ── Write (server) ────────────────────────────────────────────────────────

    public static void set(ServerPlayer player, int level) {
        CorruptionData old = data(player);
        ATTACHMENT.set(player, new CorruptionData(level, old.progress()));
    }

    /** Negative amounts reduce it. Clamped to 0–100. */
    public static void add(ServerPlayer player, int amount) {
        set(player, get(player) + amount);
    }

    /** Level AND progress back to 0. */
    public static void reset(ServerPlayer player) {
        ATTACHMENT.set(player, CorruptionData.NONE);
    }

    /**
     * Call once per tick to slowly raise corruption. After ticksPerLevel calls the level goes
     * up by 1 and progress resets. Does nothing at MAX.
     *
     * Only syncs to the client when something visible changes (the level, or the first progress
     * tick which makes the bar appear) — not 20 packets per second.
     */
    public static void addProgress(ServerPlayer player, int ticksPerLevel) {
        CorruptionData old = data(player);
        if (old.level() >= MAX) return;

        int progress = old.progress() + 1;
        if (progress >= ticksPerLevel) {
            ATTACHMENT.set(player, new CorruptionData(old.level() + 1, 0));
        } else if (old.progress() == 0) {
            ATTACHMENT.set(player, new CorruptionData(old.level(), progress));
        } else {
            ATTACHMENT.setSilently(player, new CorruptionData(old.level(), progress));
        }
    }
}
