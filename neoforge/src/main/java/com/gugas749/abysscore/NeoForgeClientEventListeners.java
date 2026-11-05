package com.gugas749.abysscore;

import com.gugas749.abysscore.api.attachment.AbyssSyncedAttachment;
import com.gugas749.abysscore.api.overlay.AbyssOverlayRenderer;
import com.gugas749.abysscore.api.permission.AbyssClientPermission;
import com.gugas749.abysscore.client.ACVanishHudHandler;
import com.gugas749.abysscore.client.ClientCommands;
import com.gugas749.abysscore.client.ClientTickHandler;
import com.gugas749.abysscore.client.CorruptionHudOverlay;
import com.gugas749.abysscore.client.ui.screens.BlindScreen;
import com.gugas749.abysscore.network.region.NoEntryHandler;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * CLIENT ONLY. Bridges NeoForge client events to the plain methods of the common client
 * classes — the same job NeoForgeEventListeners does for server events.
 *
 * Why it's needed: those common classes have no @SubscribeEvent (they can't, it's a NeoForge
 * annotation), and NeoForge's EventBus.register(obj) THROWS when obj has no @SubscribeEvent
 * method. Registering them directly crashed the client at startup.
 *
 * Registered in Abysscore.java inside the "client only" block.
 */
public class NeoForgeClientEventListeners {

    private final ClientTickHandler clientTick = new ClientTickHandler();
    private final ACVanishHudHandler vanishHud = new ACVanishHudHandler();
    private final NoEntryHandler noEntry       = new NoEntryHandler();

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        clientTick.onClientTick();
    }

    /** Before the HUD: black screen first, so the blind effect also hides chat backgrounds. */
    @SubscribeEvent
    public void onRenderGuiPre(RenderGuiEvent.Pre event) {
        BlindScreen.renderBlack(event.getGuiGraphics());
    }

    /** After the HUD. Blind goes LAST so it covers the other overlays too. */
    @SubscribeEvent
    public void onRenderGuiPost(RenderGuiEvent.Post event) {
        AbyssOverlayRenderer.render(event.getGuiGraphics());   // screen tints first, texts on top
        CorruptionHudOverlay.render(event.getGuiGraphics());
        vanishHud.render(event.getGuiGraphics(), Minecraft.getInstance());
        noEntry.onRenderGui(event.getGuiGraphics());
        BlindScreen.renderBlack(event.getGuiGraphics());
    }

    /** Leaving a world/server: forget what the last server told us. */
    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        AbyssOverlayRenderer.onLoggingOut();
        AbyssClientPermission.onLoggingOut();
        AbyssSyncedAttachment.onClientLogout();
    }

    /** /corruptionhud and /abyssgui (client-side commands). */
    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        ClientCommands.register(event.getDispatcher());
    }
}
