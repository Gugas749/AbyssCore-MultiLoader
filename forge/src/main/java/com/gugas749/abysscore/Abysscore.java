package com.gugas749.abysscore;

import com.gugas749.abysscore.AbysscoreServerConfig;
import com.gugas749.abysscore.api.corruption.AbyssCorruption;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.client.KeyBindings;
import com.gugas749.abysscore.commands.ACModCommands;
import com.gugas749.abysscore.features.bulk.BulkCommandManager;
import com.gugas749.abysscore.features.dimen.ACDimensionManager;
import com.gugas749.abysscore.features.title.ACTitleManager;
import com.gugas749.abysscore.features.vanish.VanishManager;
import com.gugas749.abysscore.network.PacketHandler;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
public class Abysscore {

    public Abysscore() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // ── Network ──────────────────────────────────────────────────────────
        // Creates the SimpleChannel and registers every packet (must happen during construction)
        PacketHandler.init();

        // ── Synced attachments + corruption ─────────────────────────────────
        // Attachments must be registered while mods are constructed; init() loads AbyssCorruption,
        // whose static field registers abysscore:corruption.
        AbyssCorruption.init();

        // ── Event bridge ─────────────────────────────────────────────────────
        // ForgeEventListeners holds instances of every common listener and
        // bridges Forge events (LivingAttackEvent, TickEvent, etc.) to their
        // plain methods. See ForgeEventListeners.java for details.
        MinecraftForge.EVENT_BUS.register(new ForgeEventListeners());

        // ── Client only ──────────────────────────────────────────────────────
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modEventBus.addListener((RegisterKeyMappingsEvent event) -> {
                for (KeyMapping key : KeyBindings.getAll()) event.register(key);
            });
            // Client tick + HUD rendering; forwards to the common client classes
            MinecraftForge.EVENT_BUS.register(new ForgeClientEventListeners());
        });

        // ── Server / common events ───────────────────────────────────────────
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);

        Constants.LOG.info("Hello Forge world!");
        CommonClass.init();
    }

    private void onServerStarting(ServerStartingEvent event) {
        AbysscoreServerConfig.load();

        BulkCommandManager.load();
        ACDimensionManager.load();
        ACDimensionManager.onServerStarted(event.getServer());
        ACTitleManager.load();
        AbyssPermissionHandler.load();
        VanishManager.load();
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ACModCommands.registerAll(event.getDispatcher());
    }
}
