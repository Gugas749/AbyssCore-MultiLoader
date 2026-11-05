package com.gugas749.abysscore.client;

import com.gugas749.abysscore.network.AbyssNetwork;
import com.gugas749.abysscore.network.binds.KeyPressPacket;
import net.minecraft.client.Minecraft;

/**
 * Fires key bindings every client tick.
 * Event wiring (NeoForge) lives in NeoForgeClientEventListeners; only plain logic here.
 */
public class ClientTickHandler {

    /** Called by NeoForgeClientEventListeners on ClientTickEvent.Post. */
    public void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;

        for (int i = 0; i < KeyBindings.SLOT_COUNT; i++) {
            if (KeyBindings.SLOTS[i].consumeClick()) {
                AbyssNetwork.sendToServer(new KeyPressPacket(i + 1));
            }
        }
    }
}
