package com.gugas749.abysscore.commands.subRegisters;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ACPermissionCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("abysscore")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("permission")
                                .then(Commands.literal("grant")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .then(Commands.argument("level", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                                java.util.Arrays.stream(AbyssPermissionLevel.values())
                                                                        .filter(l -> l != AbyssPermissionLevel.ADMIN)
                                                                        .map(Enum::name)
                                                                        .toList(),
                                                                builder
                                                        ))
                                                        .executes(ACPermissionCommands::executeGrant)
                                                )
                                        )
                                )

                                .then(Commands.literal("revoke")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ACPermissionCommands::executeRevoke)
                                        )
                                )
                        )
        );

        Constants.LOG.info("[AbyssCore] Registered: /abysscore permissions");
    }

    private static int executeGrant(CommandContext<CommandSourceStack> ctx){
        CommandSourceStack source = ctx.getSource();
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
            String levelStr = StringArgumentType.getString(ctx, "level");
            AbyssPermissionLevel level;

            try {
                level = AbyssPermissionLevel.valueOf(levelStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                source.sendFailure(Component.translatable("message.abysscore.permission.invalidlevel"));
                return 0;
            }

            AbyssPermissionHandler.grant(player.getUUID(), level);
            source.sendSuccess(() -> Component.translatable("message.abysscore.permission.grant", StringArgumentType.getString(ctx, "level"), player.getName()), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.abysscore.god.player_not_found"));
            return 0;
        }
    }

    private static int executeRevoke(CommandContext<CommandSourceStack> ctx){
        CommandSourceStack source = ctx.getSource();
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
            AbyssPermissionHandler.revoke(player.getUUID());
            source.sendSuccess(() -> Component.translatable("message.abysscore.permission.revoke", player.getName()), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.abysscore.god.player_not_found"));
            return 0;
        }
    }
}
