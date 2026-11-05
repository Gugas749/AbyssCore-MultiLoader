package com.gugas749.abysscore.client;

import com.gugas749.abysscore.client.gui.demo.AbyssGuiDemoCommand;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

/**
 * CLIENT. Every client-side command (they run on the player's own game; the server never sees
 * them). Each loader calls register() from its RegisterClientCommandsEvent listener.
 */
public final class ClientCommands {

    private ClientCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CorruptionHudOverlay.registerCommand(dispatcher);   // /corruptionhud
        AbyssGuiDemoCommand.register(dispatcher);           // /abyssgui
    }
}
