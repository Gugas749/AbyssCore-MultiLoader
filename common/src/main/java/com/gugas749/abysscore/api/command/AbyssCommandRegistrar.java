package com.gugas749.abysscore.api.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public abstract class AbyssCommandRegistrar {

    /*
     *       EXAMPLE OF USE
     *
     *    public class ModCommands extends AbyssCommandRegistrar {
     *        @Override
     *        protected List<AbyssCommand> commands() {
     *            return List.of(
     *                SomeCommand::register,
     *                OtherCommand::register
     *            );
     *        }
     *    }
     *
     * Then in the NeoForge loader, on RegisterCommandsEvent:
     *    new ModCommands().registerAll(event.getDispatcher());
     *
     * Forge 1.20.1 mod constructor:
     *    MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
     *            new ModCommands().registerAll(event.getDispatcher()));
     *
     * Why "registerAll" and not "register": command classes have a STATIC
     * register(CommandDispatcher). A class that is both a command and the registrar would then
     * have a static and an inherited instance method with the same signature — a compile error.
     */

    protected abstract List<AbyssCommand> commands();

    /** Called from the loader's RegisterCommandsEvent handler. */
    public void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        commands().forEach(cmd -> cmd.register(dispatcher));
    }
}
