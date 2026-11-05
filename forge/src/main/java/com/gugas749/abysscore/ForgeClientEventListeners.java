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
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * CLIENT ONLY. Forge version of NeoForgeClientEventListeners: bridges client events to the
 * plain methods of the common client classes (they have no @SubscribeEvent of their own).
 */
public class ForgeClientEventListeners {

    private final ClientTickHandler clientTick = new ClientTickHandler();
    private final ACVanishHudHandler vanishHud = new ACVanishHudHandler();
    private final NoEntryHandler noEntry       = new NoEntryHandler();

    /** Forge fires the client tick twice (START and END); run once, at END. */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        clientTick.onClientTick();
    }

    @SubscribeEvent
    public void onRenderGuiPre(RenderGuiEvent.Pre event) {
        BlindScreen.renderBlack(event.getGuiGraphics());
    }

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
