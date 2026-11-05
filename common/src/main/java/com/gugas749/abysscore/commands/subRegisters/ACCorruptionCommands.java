package com.gugas749.abysscore.commands.subRegisters;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.api.corruption.AbyssCorruption;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * ABY-16 — /abysscore corruption check|set|add|reset <player> [value]   (ADMIN)
 *
 * Note where .requires() is: on the "corruption" node, NOT on "abysscore". Brigadier merges
 * every dispatcher.register(literal("abysscore")...) into ONE node, and that node keeps the
 * requirement from the FIRST registration (ACModCommands -> MODERATOR). A .requires() on the
 * root here would be silently ignored.
 */
public class ACCorruptionCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("abysscore")
                        .then(Commands.literal("corruption")
                                .requires(source -> AbyssPermissionHandler.sourceHas(source, AbyssPermissionLevel.ADMIN))

                                .then(Commands.literal("check")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ACCorruptionCommands::executeCheck)))

                                .then(Commands.literal("set")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0, AbyssCorruption.MAX))
                                                        .executes(ACCorruptionCommands::executeSet))))

                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("value", IntegerArgumentType.integer(-AbyssCorruption.MAX, AbyssCorruption.MAX))
                                                        .executes(ACCorruptionCommands::executeAdd))))

                                .then(Commands.literal("reset")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ACCorruptionCommands::executeReset)))
                        )
        );

        Constants.LOG.info("[AbyssCore] Registered: /abysscore corruption <check|set|add|reset>");
    }

    private static int executeCheck(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int level = AbyssCorruption.get(target);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.abysscore.corruption.check",
                target.getName().getString(), String.valueOf(level)), false);
        return level;
    }

    private static int executeSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        AbyssCorruption.set(target, IntegerArgumentType.getInteger(ctx, "value"));
        return feedback(ctx, target);
    }

    private static int executeAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        AbyssCorruption.add(target, IntegerArgumentType.getInteger(ctx, "value"));
        return feedback(ctx, target);
    }

    private static int executeReset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        AbyssCorruption.reset(target);
        return feedback(ctx, target);
    }

    private static int feedback(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        int level = AbyssCorruption.get(target);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.abysscore.corruption.set",
                target.getName().getString(), String.valueOf(level)), true);
        return level;
    }
}
