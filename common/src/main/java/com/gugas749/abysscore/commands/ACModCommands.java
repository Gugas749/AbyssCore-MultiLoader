package com.gugas749.abysscore.commands;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.AbysscoreServerConfig;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import com.gugas749.abysscore.commands.subRegisters.*;
import com.gugas749.abysscore.features.bulk.BulkCommandManager;
import com.gugas749.abysscore.features.chat.ACChatLockListener;
import com.gugas749.abysscore.features.title.ACTitleManager;
import com.gugas749.abysscore.network.AbyssNetwork;
import com.gugas749.abysscore.network.menu.MenuPacketHandlers;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ACModCommands {

    /** Called from the loader's RegisterCommandsEvent to register all common commands. */
    public static void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        Constants.LOG.info("[AbyssCore] Registering commands...");

        register(dispatcher);

        ACBindCommands.register(dispatcher);
        ACBlindCommands.register(dispatcher);
        ACBulkCommands.register(dispatcher);
        ACCorruptionCommands.register(dispatcher);
        ACDimenCommands.register(dispatcher);
        ACDimenSettingsCommands.register(dispatcher);
        ACGodCommands.register(dispatcher);
        ACHelpCommands.register(dispatcher);
        ACPermissionCommands.register(dispatcher);
        ACRegionCommands.register(dispatcher);
        ACVanishCommands.register(dispatcher);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("abysscore")
                        .requires(source -> AbyssPermissionHandler.sourceHas(source, AbyssPermissionLevel.MODERATOR))
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) return 0;
                            AbyssNetwork.sendToPlayer(player,
                                    MenuPacketHandlers.buildMenuPacket(player));
                            return 1;
                        })

                        .then(Commands.literal("reload")
                                .executes(ctx -> {
                                    AbysscoreServerConfig.load();
                                    AbyssPermissionHandler.load();
                                    ACTitleManager.load();
                                    BulkCommandManager.load();
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("[AbyssCore] Reloaded."), true);
                                    return 1;
                                })
                        )

                        .then(Commands.literal("chat")
                                .requires(source -> AbyssPermissionHandler.sourceHas(source, AbyssPermissionLevel.ADMIN))
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) return 0;
                                    ACChatLockListener.toggleAdminMute(player.getUUID());
                                    boolean muted = ACChatLockListener.isAdminMuted(player.getUUID());
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("[AbyssCore] Your chat is now " + (muted ? "muted" : "active") + "."),
                                            false);
                                    return 1;
                                })
                        )
        );

        Constants.LOG.info("[AbyssCore] Registered: /abysscore");
    }

}
