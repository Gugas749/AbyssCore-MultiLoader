package com.gugas749.abysscore.features.vanish;

import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The logic behind the vanish mixins. Each mixin is one line that calls a method here, so
 * all decisions stay readable in one place (same idea as "loader listeners only forward").
 */
public final class VanishHooks {

    private VanishHooks() {}

    //-----------------------------------------------------------------------------------
    //                         TAB LIST (outgoing packet filter)
    //-----------------------------------------------------------------------------------

    /**
     * Called for EVERY packet sent to {@code receiver}.
     * Returns the same packet (send it), a new packet (send that instead) or null (send nothing).
     *
     * Only player-info packets are touched: they put players in the tab list and are sent
     * on join, every 30s (ping), on game mode / display name changes...
     */
    @Nullable
    public static Packet<?> filterOutgoing(ServerPlayer receiver, Packet<?> packet) {
        if (VanishManager.nobodyVanished()) return packet;
        if (!(packet instanceof ClientboundPlayerInfoUpdatePacket info)) return packet;

        PlayerList playerList = receiver.getServer().getPlayerList();
        List<ServerPlayer> keep = new ArrayList<>();
        boolean removedSomeone = false;

        for (ClientboundPlayerInfoUpdatePacket.Entry entry : info.entries()) {
            ServerPlayer player = playerList.getPlayer(entry.profileId());
            if (player == null) continue;   // not online anymore, nothing to show
            if (VanishManager.canSee(receiver, player)) keep.add(player);
            else removedSomeone = true;
        }

        if (!removedSomeone) return packet;
        if (keep.isEmpty()) return null;
        // The entries' fields are private, so rebuild the packet from the players that stay.
        // Same tick, same players → the same data vanilla would have sent.
        return new ClientboundPlayerInfoUpdatePacket(info.actions(), keep);
    }

    //-----------------------------------------------------------------------------------
    //                        SOUNDS & BLOCK EFFECTS (SILENT_SOUNDS)
    //-----------------------------------------------------------------------------------

    /**
     * PlayerList.broadcast(except, x, y, z, radius, dimension, packet) sends sounds and
     * level events (block break particles...) caused by {@code except} to everyone near.
     * If {@code except} is a vanished player, we send it only to who can see them.
     * True = we handled it, cancel vanilla.
     */
    public static boolean broadcastNear(PlayerList list, @Nullable Player except, double x, double y, double z,
                                        double radius, ResourceKey<Level> dimension, Packet<?> packet) {
        if (!VanishManager.isActive(except, VanishOption.SILENT_SOUNDS)) return false;

        for (ServerPlayer player : list.getPlayers()) {
            if (player == except || player.level().dimension() != dimension) continue;
            double dx = x - player.getX();
            double dy = y - player.getY();
            double dz = z - player.getZ();
            if (dx * dx + dy * dy + dz * dz < radius * radius && VanishManager.canSee(player, except)) {
                player.connection.send(packet);
            }
        }
        return true;
    }

    //-----------------------------------------------------------------------------------
    //                               SERVER LIST (ping)
    //-----------------------------------------------------------------------------------

    /** Removes online vanished players from the player count and the hover list. Null = unchanged. */
    @Nullable
    public static ServerStatus.Players filterStatus(PlayerList list, ServerStatus.Players players) {
        if (VanishManager.nobodyVanished()) return null;

        int hiddenOnline = 0;
        for (ServerPlayer player : list.getPlayers()) {
            if (VanishManager.isVanished(player)) hiddenOnline++;
        }
        if (hiddenOnline == 0) return null;

        List<GameProfile> sample = new ArrayList<>();
        for (GameProfile profile : players.sample()) {
            if (!VanishManager.isVanished(profile.getId())) sample.add(profile);
        }
        return new ServerStatus.Players(players.max(), Math.max(0, players.online() - hiddenOnline), sample);
    }

    //-----------------------------------------------------------------------------------
    //                          COMMAND SELECTORS (@a, names)
    //-----------------------------------------------------------------------------------

    /**
     * When a PLAYER runs a command, vanished players they can't see are left out of the
     * selector result, so "/msg Name hi" or "/tp @a" doesn't give the vanished player away.
     * Console, command blocks and staff are not affected. Null = unchanged.
     */
    @Nullable
    public static <T extends Entity> List<T> filterSelected(CommandSourceStack source, List<T> found) {
        if (VanishManager.nobodyVanished() || found.isEmpty()) return null;
        if (!(source.getEntity() instanceof ServerPlayer viewer)) return null;

        List<T> visible = new ArrayList<>(found.size());
        for (T entity : found) {
            if (VanishManager.canSee(viewer, entity)) visible.add(entity);
        }
        return visible.size() == found.size() ? null : visible;
    }

    //-----------------------------------------------------------------------------------
    //                         CONTAINERS (SILENT_CONTAINERS)
    //-----------------------------------------------------------------------------------

    public static boolean isSilentOpener(Player player) {
        return VanishManager.isActive(player, VanishOption.SILENT_CONTAINERS);
    }
}
