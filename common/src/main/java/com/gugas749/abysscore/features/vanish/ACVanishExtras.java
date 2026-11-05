package com.gugas749.abysscore.features.vanish;

import com.gugas749.abysscore.Constants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Visibility exceptions and team visibility. VanishManager.canSee() reads them.
 *
 *   showTo(A, B)  → vanished A is visible to B, even without permission
 *   hideTo(A, B)  → vanished A is hidden from B, even if B is staff or a teammate
 *
 * Exceptions live in memory only and are cleared when either player leaves.
 * Team visibility is saved with the rest of the vanish data.
 */
public class ACVanishExtras {

    // volatile + concurrent: read by Simple Voice Chat's thread too (see VanishManager)
    private static volatile boolean teamVisibilityEnabled = true;
    private static final Map<UUID, Set<UUID>> explicitlyShownTo = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> explicitlyHiddenFrom = new ConcurrentHashMap<>();

    // ── Team visibility ───────────────────────────────────────────────────────

    public static boolean isTeamVisibilityEnabled() {
        return teamVisibilityEnabled;
    }

    /** Only for VanishManager.load() — no save, no refresh. */
    static void setTeamVisibility(boolean enabled) {
        teamVisibilityEnabled = enabled;
    }

    public static boolean toggleTeamVisibility(MinecraftServer server) {
        teamVisibilityEnabled = !teamVisibilityEnabled;
        VanishManager.save();
        VanishManager.refreshAll(server);
        Constants.LOG.info("[AbyssCore] Vanish team visibility: {}", teamVisibilityEnabled ? "ON" : "OFF");
        return teamVisibilityEnabled;
    }

    // ── Explicit show / hide ──────────────────────────────────────────────────

    public static void showTo(UUID vanishedPlayer, UUID viewer, MinecraftServer server) {
        explicitlyShownTo.computeIfAbsent(vanishedPlayer, k -> ConcurrentHashMap.newKeySet()).add(viewer);
        Set<UUID> hidden = explicitlyHiddenFrom.get(vanishedPlayer);
        if (hidden != null) hidden.remove(viewer);
        refresh(vanishedPlayer, server);
    }

    public static void hideTo(UUID vanishedPlayer, UUID viewer, MinecraftServer server) {
        explicitlyHiddenFrom.computeIfAbsent(vanishedPlayer, k -> ConcurrentHashMap.newKeySet()).add(viewer);
        Set<UUID> shown = explicitlyShownTo.get(vanishedPlayer);
        if (shown != null) shown.remove(viewer);
        refresh(vanishedPlayer, server);
    }

    public static void clearExceptions(UUID vanishedPlayer, MinecraftServer server) {
        explicitlyShownTo.remove(vanishedPlayer);
        explicitlyHiddenFrom.remove(vanishedPlayer);
        refresh(vanishedPlayer, server);
    }

    public static void onPlayerLeave(UUID uuid) {
        explicitlyShownTo.remove(uuid);
        explicitlyHiddenFrom.remove(uuid);
        explicitlyShownTo.values().forEach(set -> set.remove(uuid));
        explicitlyHiddenFrom.values().forEach(set -> set.remove(uuid));
    }

    /** Applies the change now (before: reflection into Vanishmod; now: our own refresh). */
    private static void refresh(UUID vanishedPlayer, MinecraftServer server) {
        if (server == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(vanishedPlayer);
        if (player != null && VanishManager.isVanished(player)) {
            VanishManager.refresh(player);
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * TRUE = forced visible, FALSE = forced hidden, null = no exception (normal rules).
     * Team visibility is NOT handled here anymore; it's a step in VanishManager.canSee().
     */
    public static Boolean checkException(UUID vanishedPlayer, UUID viewer) {
        Set<UUID> hiddenFrom = explicitlyHiddenFrom.get(vanishedPlayer);
        if (hiddenFrom != null && hiddenFrom.contains(viewer)) return Boolean.FALSE;

        Set<UUID> shownTo = explicitlyShownTo.get(vanishedPlayer);
        if (shownTo != null && shownTo.contains(viewer)) return Boolean.TRUE;

        return null;
    }

    public static Set<UUID> getShownTo(UUID vanishedPlayer) {
        return Collections.unmodifiableSet(
            explicitlyShownTo.getOrDefault(vanishedPlayer, Collections.emptySet()));
    }

    public static Set<UUID> getHiddenFrom(UUID vanishedPlayer) {
        return Collections.unmodifiableSet(
            explicitlyHiddenFrom.getOrDefault(vanishedPlayer, Collections.emptySet()));
    }
}
