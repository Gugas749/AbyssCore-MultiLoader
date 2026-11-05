package com.gugas749.abysscore.api.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public interface AbyssCommand {
    void register(CommandDispatcher<CommandSourceStack> dispatcher);
}
