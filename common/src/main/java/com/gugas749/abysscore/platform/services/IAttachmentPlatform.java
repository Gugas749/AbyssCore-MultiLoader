package com.gugas749.abysscore.platform.services;

import com.gugas749.abysscore.api.attachment.AttachmentStorage;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

/**
 * Loader-specific saving of per-player data, loaded with Services (META-INF/services in each loader).
 *   NeoForge 1.21.1 → NeoForgeAttachmentPlatform (data attachments)
 *   Forge 1.20.1    → ForgeAttachmentPlatform    (persistent NBT)
 *
 * Common code doesn't call this directly: use AbyssSyncedAttachment.register(...).
 */
public interface IAttachmentPlatform {

    /**
     * Called while mods are being constructed (AbyssSyncedAttachment.register in a static field).
     * NeoForge needs that timing: attachment types are registered in a registry event.
     */
    <T> AttachmentStorage<T> createStorage(ResourceLocation id, T defaultValue, Codec<T> codec);
}
