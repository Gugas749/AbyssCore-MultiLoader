package com.gugas749.abysscore.client.gui.demo;

import com.gugas749.abysscore.api.permission.AbyssClientPermission;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /abyssgui — a CLIENT command (runs on the player's own game, the server never sees it).
 * Opens the GUI demo for staff.
 * Called from NeoForgeClientEventListeners on RegisterClientCommandsEvent.
 */
public final class AbyssGuiDemoCommand {

    private AbyssGuiDemoCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("abyssgui").executes(ctx -> {
            if (!AbyssClientPermission.has(AbyssPermissionLevel.MODERATOR)) {
                ctx.getSource().sendFailure(Component.translatable("gui.abysscore.no_permission"));
                return 0;
            }
            // The chat screen closes itself AFTER running the command, which would close our
            // screen immediately. tell() runs it on the next frame, after chat has closed.
            Minecraft mc = Minecraft.getInstance();
            mc.tell(() -> mc.setScreen(new AbyssGuiDemoScreen()));
            return 1;
        }));
    }
}
