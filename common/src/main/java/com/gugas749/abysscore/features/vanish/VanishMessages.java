package com.gugas749.abysscore.features.vanish;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;

/**
 * Hides "X joined / left / died / made the advancement" messages of vanished players
 * (option SILENT_MESSAGES). Staff who can see them still get the message, with " (vanished)".
 *
 * The problem: vanilla builds these messages in different places, but they all end up in
 * PlayerList.broadcastSystemMessage(...), which doesn't know WHICH player the message is about.
 *
 * The trick, a "subject":
 *   1. Mixins on the methods that send those messages (placeNewPlayer, removePlayerFromWorld,
 *      ServerPlayer.die, PlayerAdvancements.award) call push(player) at HEAD and pop() at RETURN.
 *   2. A mixin at HEAD of broadcastSystemMessage calls handleBroadcast(). If the current subject
 *      is vanished, we send the message only to who can see them and cancel the vanilla broadcast.
 *
 * Why a stack and not one field? These methods can run INSIDE each other (an advancement
 * granted while placeNewPlayer runs). With one field the inner pop() would wipe the outer
 * subject; with a stack the outer one comes back.
 *
 * Minecraft's server logic runs on ONE thread, so no locking is needed.
 * If one of those methods throws, RETURN never runs: ACVanishStateListener.onPlayerTick
 * calls reset() as a safety net.
 */
public final class VanishMessages {

    private static final Deque<ServerPlayer> subjects = new ArrayDeque<>();

    private VanishMessages() {}

    public static void push(ServerPlayer player) {
        subjects.push(player);
    }

    public static void pop() {
        subjects.poll();   // poll, not pop: no exception if it's already empty
    }

    public static void reset() {
        subjects.clear();
    }

    /** HEAD of PlayerList.broadcastSystemMessage(Component, Function, boolean). True = cancel vanilla. */
    public static boolean handleBroadcast(PlayerList list, Component consoleMessage,
                                          Function<ServerPlayer, Component> perPlayer, boolean bypassHiddenChat) {
        ServerPlayer who = subjects.peek();
        if (!VanishManager.isActive(who, VanishOption.SILENT_MESSAGES)) return false;

        list.getServer().sendSystemMessage(withSuffix(consoleMessage));   // server log, like vanilla
        for (ServerPlayer viewer : list.getPlayers()) {
            if (!VanishManager.canSee(viewer, who)) continue;
            Component message = perPlayer.apply(viewer);
            if (message != null) viewer.sendSystemMessage(withSuffix(message), bypassHiddenChat);
        }
        return true;
    }

    /**
     * HEAD of broadcastSystemToTeam / broadcastSystemToAllExceptTeam (death messages when the
     * team's "deathMessageVisibility" rule is set). Here vanilla tells us the player directly.
     */
    public static boolean handleTeamBroadcast(PlayerList list, Player who, Component message, boolean onlyTeam) {
        if (!VanishManager.isActive(who, VanishOption.SILENT_MESSAGES)) return false;

        list.getServer().sendSystemMessage(withSuffix(message));
        for (ServerPlayer viewer : list.getPlayers()) {
            if (viewer == who || !VanishManager.canSee(viewer, who)) continue;
            boolean sameTeam = who.getTeam() != null && viewer.getTeam() == who.getTeam();
            if (sameTeam == onlyTeam) viewer.sendSystemMessage(withSuffix(message));
        }
        return true;
    }

    private static Component withSuffix(Component message) {
        return Component.empty().append(message)
                .append(Component.translatable("message.abysscore.vanish.staff_suffix").withStyle(ChatFormatting.GRAY));
    }
}
