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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(Constants.MOD_ID)
public class Abysscore {

    public Abysscore(IEventBus eventBus) {
        // ── Synced attachments + corruption ─────────────────────────────────
        // Attachments must be registered while mods are constructed; init() loads AbyssCorruption,
        // whose static field registers abysscore:corruption.
        AbyssCorruption.init();

        // ── Network ──────────────────────────────────────────────────────────
        // Creates the channel; NeoForgeNetworkPlatform registers the packets in RegisterPayloadHandlersEvent
        PacketHandler.init();

        // ── Event bridge ─────────────────────────────────────────────────────
        // NeoForgeEventListeners bridges NeoForge game events to the plain
        // methods on common listener classes (god, regions, vanish, etc.).
        NeoForge.EVENT_BUS.register(new NeoForgeEventListeners());

        // ── Client only ──────────────────────────────────────────────────────
        if (FMLEnvironment.dist == Dist.CLIENT) {
            eventBus.addListener((RegisterKeyMappingsEvent event) -> {
                for (KeyMapping key : KeyBindings.getAll()) event.register(key);
            });
            // Client tick + HUD rendering (vanish HUD, no-entry fade, blind screen).
            // The common client classes have no @SubscribeEvent, so this class forwards to them.
            NeoForge.EVENT_BUS.register(new NeoForgeClientEventListeners());
        }

        // ── Server / common events ───────────────────────────────────────────
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        Constants.LOG.info("Hello NeoForge world!");
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

    private void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        ACModCommands.registerAll(event.getDispatcher());
    }
}
