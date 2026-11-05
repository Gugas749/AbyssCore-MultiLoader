package com.gugas749.abysscore.features.vanish;

import java.util.Locale;

/**
 * Every per-player vanish setting. Each staff member picks their own.
 *
 * To add a new option: add a constant here (at the END, so saved bitmasks stay valid),
 * add its two lang keys, and check it where it matters with
 * VanishManager.isActive(player, VanishOption.YOUR_OPTION).
 * The GUI and the /abysscore vanish option command pick it up automatically.
 */
public enum VanishOption {
    HUD_WARNING      (true),   // "You are vanished" text on your screen
    CHAT_NOTICE      (true),   // chat message when you vanish / reappear / join vanished
    SILENT_MESSAGES  (true),   // your join/leave/death/advancement messages only go to staff
    FAKE_MESSAGES    (false),  // fake "left the game" / "joined the game" when you toggle
    BLOCK_CHAT       (false),  // stops you from talking in public chat by accident
    NO_PICKUP        (true),   // you don't pick up items, arrows or XP (and orbs don't fly to you)
    MOBS_IGNORE      (true),   // mobs don't target you
    NO_TRIGGERS      (true),   // pressure plates, tripwires and sculk ignore you
    NO_COLLISION     (true),   // you don't push entities and they don't push you
    SILENT_SOUNDS    (true),   // your sounds (steps, blocks, eating) only for who can see you
    SILENT_CONTAINERS(true),   // chests, barrels, ender chests and shulker boxes: no animation/sound
    MUTE_VOICE       (true),   // Simple Voice Chat: who can't see you can't hear you
    NIGHT_VISION     (false);  // night vision while vanished

    public final boolean defaultValue;

    VanishOption(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    /** One bit per option, so the whole set fits in one int for packets. */
    public int bit() {
        return 1 << ordinal();
    }

    /** "silent_messages" — used in commands, the JSON save file and lang keys. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String labelKey()       { return "gui.abysscore.vanish.option." + id(); }
    public String descriptionKey() { return "gui.abysscore.vanish.option." + id() + ".desc"; }

    /** Returns null for an unknown id (old save file, typo in a command). */
    public static VanishOption byId(String id) {
        for (VanishOption option : values()) {
            if (option.id().equalsIgnoreCase(id)) return option;
        }
        return null;
    }
}
