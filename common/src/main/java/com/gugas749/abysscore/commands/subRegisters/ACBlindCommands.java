package com.gugas749.abysscore.commands.subRegisters;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import com.gugas749.abysscore.features.blind.ACBlindManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ACBlindCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("abysscore")
                .requires(source -> AbyssPermissionHandler.sourceHas(source, AbyssPermissionLevel.MODERATOR))

                .then(Commands.literal("blind")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ACBlindCommands::executeBlind)
                    )
                )

                .then(Commands.literal("unblind")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ACBlindCommands::executeUnblind)
                    )
                )
        );

        Constants.LOG.info("[AbyssCore] Registered: /abysscore blind|unblind <player>");
    }

    private static int executeBlind(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            ACBlindManager.blind(target);
            ctx.getSource().sendSuccess(() -> Component.translatable("message.abysscore.blind.enabled", target.getName().getString()),
                true
            );
            target.sendSystemMessage(
                Component.translatable("message.abysscore.blind.enabled_self"));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(
                Component.translatable("message.abysscore.blind.player_not_found"));
            return 0;
        }
    }

    private static int executeUnblind(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            ACBlindManager.unblind(target);
            ctx.getSource().sendSuccess(
                () -> Component.translatable("message.abysscore.blind.disabled",
                    target.getName().getString()),
                true
            );
            target.sendSystemMessage(
                Component.translatable("message.abysscore.blind.disabled_self"));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(
                Component.translatable("message.abysscore.blind.player_not_found"));
            return 0;
        }
    }
}
