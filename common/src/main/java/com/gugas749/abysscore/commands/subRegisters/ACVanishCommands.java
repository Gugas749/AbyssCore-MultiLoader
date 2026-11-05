package com.gugas749.abysscore.commands.subRegisters;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import com.gugas749.abysscore.features.vanish.ACVanishExtras;
import com.gugas749.abysscore.features.vanish.VanishManager;
import com.gugas749.abysscore.features.vanish.VanishOption;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

/**
 * /abysscore vanish                          toggle yourself
 * /abysscore vanish on | off                 set yourself
 * /abysscore vanish toggle <players>         toggle other players
 * /abysscore vanish list                     who is vanished
 * /abysscore vanish option                   your options
 * /abysscore vanish option <name> [true|false]   flip or set one option
 * /abysscore vanish teamvisibility           teammates can / can't see vanished players
 * /abysscore vanish show|hide <player>       per-player exception for YOU
 * /abysscore vanish clearexceptions | status
 */
public class ACVanishCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("abysscore")
                // .requires on the SUB-literal: Brigadier merges every literal("abysscore")
                // and keeps only the first root .requires (ACModCommands' MODERATOR one).
                .then(Commands.literal("vanish")
                    .requires(source -> AbyssPermissionHandler.sourceHas(source, AbyssPermissionLevel.ADMIN))
                    .executes(ctx -> setSelf(ctx, null))

                    .then(Commands.literal("on").executes(ctx -> setSelf(ctx, true)))
                    .then(Commands.literal("off").executes(ctx -> setSelf(ctx, false)))

                    .then(Commands.literal("toggle")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .executes(ACVanishCommands::executeToggleOthers)))

                    .then(Commands.literal("list").executes(ACVanishCommands::executeList))

                    .then(Commands.literal("option")
                        .executes(ACVanishCommands::executeListOptions)
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                    Arrays.stream(VanishOption.values()).map(VanishOption::id), builder))
                            .executes(ctx -> executeOption(ctx, null))
                            .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> executeOption(ctx, BoolArgumentType.getBool(ctx, "value"))))))

                    .then(Commands.literal("teamvisibility").executes(ACVanishCommands::executeTeamVisibility))

                    .then(Commands.literal("show")
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(ctx -> executeException(ctx, true))))

                    .then(Commands.literal("hide")
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(ctx -> executeException(ctx, false))))

                    .then(Commands.literal("clearexceptions").executes(ACVanishCommands::executeClear))

                    .then(Commands.literal("status").executes(ACVanishCommands::executeStatus))
                )
        );

        Constants.LOG.info("[AbyssCore] Registered: /abysscore vanish <on|off|toggle|list|option|teamvisibility|show|hide|clearexceptions|status>");
    }

    // ── Toggle ────────────────────────────────────────────────────────────────

    /** value null = toggle. The player gets the CHAT_NOTICE message from VanishManager. */
    private static int setSelf(CommandContext<CommandSourceStack> ctx, Boolean value) {
        ServerPlayer self = playerOrFail(ctx.getSource());
        if (self == null) return 0;
        boolean target = value != null ? value : !VanishManager.isVanished(self.getUUID());
        VanishManager.setVanished(self, target);
        return 1;
    }

    private static int executeToggleOthers(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer target : targets) {
            boolean nowVanished = VanishManager.toggle(target);
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    nowVanished ? "message.abysscore.vanish.other_vanished" : "message.abysscore.vanish.other_visible",
                    target.getName().getString()), true);
        }
        return targets.size();
    }

    // ── List ──────────────────────────────────────────────────────────────────

    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        var vanished = VanishManager.getVanished();
        if (vanished.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("message.abysscore.vanish.list_empty"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("message.abysscore.vanish.list_header", vanished.size()), false);
        for (UUID uuid : vanished) {
            ServerPlayer p = source.getServer().getPlayerList().getPlayer(uuid);
            Component line = p != null
                    ? Component.literal("  - ").append(Component.literal(p.getName().getString()).withStyle(ChatFormatting.WHITE))
                    : Component.literal("  - " + uuid + " ").append(Component.translatable("message.abysscore.vanish.offline").withStyle(ChatFormatting.DARK_GRAY));
            source.sendSuccess(() -> line, false);
        }
        return vanished.size();
    }

    // ── Options ───────────────────────────────────────────────────────────────

    private static int executeListOptions(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer self = playerOrFail(ctx.getSource());
        if (self == null) return 0;
        ctx.getSource().sendSuccess(() -> Component.translatable("message.abysscore.vanish.options_header"), false);
        for (VanishOption option : VanishOption.values()) {
            boolean on = VanishManager.getOption(self.getUUID(), option);
            Component line = Component.literal("  " + option.id() + ": ").withStyle(ChatFormatting.GRAY)
                    .append(onOff(on))
                    .append(Component.literal("  ").append(Component.translatable(option.descriptionKey())).withStyle(ChatFormatting.DARK_GRAY));
            ctx.getSource().sendSuccess(() -> line, false);
        }
        return 1;
    }

    /** value null = flip the current value. */
    private static int executeOption(CommandContext<CommandSourceStack> ctx, Boolean value) {
        ServerPlayer self = playerOrFail(ctx.getSource());
        if (self == null) return 0;

        String name = StringArgumentType.getString(ctx, "name");
        VanishOption option = VanishOption.byId(name);
        if (option == null) {
            ctx.getSource().sendFailure(Component.translatable("message.abysscore.vanish.option_unknown", name));
            return 0;
        }
        boolean newValue = value != null ? value : !VanishManager.getOption(self.getUUID(), option);
        VanishManager.setOption(self, option, newValue);
        ctx.getSource().sendSuccess(() -> Component.translatable("message.abysscore.vanish.option_set",
                Component.translatable(option.labelKey()), onOff(newValue)), false);
        return 1;
    }

    // ── Team visibility ───────────────────────────────────────────────────────

    private static int executeTeamVisibility(CommandContext<CommandSourceStack> ctx) {
        boolean nowEnabled = ACVanishExtras.toggleTeamVisibility(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.translatable(
                nowEnabled ? "message.abysscore.vanish.team_visible" : "message.abysscore.vanish.team_hidden"), true);
        return 1;
    }

    // ── Exceptions ────────────────────────────────────────────────────────────

    /** show = true → /vanish show, false → /vanish hide. Both are about YOU (the sender). */
    private static int executeException(CommandContext<CommandSourceStack> ctx, boolean show) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer self = playerOrFail(source);
        if (self == null) return 0;
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            if (target.equals(self)) {
                source.sendFailure(Component.translatable("message.abysscore.vanish.cannot_target_self"));
                return 0;
            }
            if (show) ACVanishExtras.showTo(self.getUUID(), target.getUUID(), source.getServer());
            else ACVanishExtras.hideTo(self.getUUID(), target.getUUID(), source.getServer());
            source.sendSuccess(() -> Component.translatable(
                    show ? "message.abysscore.vanish.shown_to" : "message.abysscore.vanish.hidden_from",
                    target.getName().getString()), false);
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.translatable("message.abysscore.vanish.player_not_found"));
            return 0;
        }
    }

    private static int executeClear(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer self = playerOrFail(ctx.getSource());
        if (self == null) return 0;
        ACVanishExtras.clearExceptions(self.getUUID(), ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.translatable("message.abysscore.vanish.exceptions_cleared"), false);
        return 1;
    }

    private static int executeStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer self = playerOrFail(source);
        if (self == null) return 0;

        source.sendSuccess(() -> Component.translatable(VanishManager.isVanished(self.getUUID())
                ? "message.abysscore.vanish.status_vanished" : "message.abysscore.vanish.status_visible"), false);
        source.sendSuccess(() -> Component.translatable(ACVanishExtras.isTeamVisibilityEnabled()
                ? "message.abysscore.vanish.team_status_on" : "message.abysscore.vanish.team_status_off"), false);

        var shownTo    = ACVanishExtras.getShownTo(self.getUUID());
        var hiddenFrom = ACVanishExtras.getHiddenFrom(self.getUUID());
        if (shownTo.isEmpty() && hiddenFrom.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("message.abysscore.vanish.no_exceptions"), false);
            return 1;
        }
        shownTo.forEach(uuid -> source.sendSuccess(() -> Component.translatable(
                "message.abysscore.vanish.status_shown", name(source, uuid)), false));
        hiddenFrom.forEach(uuid -> source.sendSuccess(() -> Component.translatable(
                "message.abysscore.vanish.status_hidden", name(source, uuid)), false));
        return 1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ServerPlayer playerOrFail(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) return player;
        source.sendFailure(Component.translatable("message.abysscore.vanish.player_only"));
        return null;
    }

    private static String name(CommandSourceStack source, UUID uuid) {
        ServerPlayer p = source.getServer().getPlayerList().getPlayer(uuid);
        return p != null ? p.getName().getString() : uuid.toString();
    }

    private static Component onOff(boolean on) {
        return Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
    }
}
