package com.gugas749.abysscore.api.attachment;

import net.minecraft.world.entity.player.Player;

/**
 * Where one synced attachment's value is SAVED on the server. Made by the loader
 * (IAttachmentPlatform.createStorage):
 *   NeoForge → a data attachment (AttachmentType), saved with the player by NeoForge
 *   Forge    → the player's persistent NBT (player.getPersistentData()), saved with the player by Forge
 *
 * AbyssSyncedAttachment does everything else (syncing, client copy, copy on respawn).
 */
public interface AttachmentStorage<T> {

    T get(Player player);

    void set(Player player, T value);
}
