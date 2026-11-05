package com.gugas749.abysscore.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final String CATEGORY = "key.categories.abysscore";
    public static final int SLOT_COUNT  = 9;

    public static final KeyMapping[] SLOTS = new KeyMapping[SLOT_COUNT];

    static {
        for (int i = 0; i < SLOT_COUNT; i++) {
            SLOTS[i] = new KeyMapping(
                    "key.abysscore.bind_" + (i + 1),          // translation key
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,                      // unbound by default
                    CATEGORY
            );
        }
    }

    /** Called by the NeoForge loader on RegisterKeyMappingsEvent to register all slots. */
    public static KeyMapping[] getAll() {
        return SLOTS;
    }
}
