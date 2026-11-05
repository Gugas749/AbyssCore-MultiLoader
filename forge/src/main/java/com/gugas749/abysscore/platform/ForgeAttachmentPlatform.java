package com.gugas749.abysscore.platform;

import com.gugas749.abysscore.api.attachment.AttachmentNbt;
import com.gugas749.abysscore.api.attachment.AttachmentStorage;
import com.gugas749.abysscore.platform.services.IAttachmentPlatform;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Forge 1.20.1 has no data attachments. Every entity has a Forge "persistent data" CompoundTag
 * (player.getPersistentData()) that Forge saves with the player, so each value is stored there:
 *
 *   persistentData / "abysscore_attachments" / "<namespace>:<name>" = {"v": <encoded value>}
 *
 * Same layout as the forge-1.20.1 branch, so data saved by the old Forge version still loads.
 * Registered via forge/src/main/resources/META-INF/services/...IAttachmentPlatform.
 */
public class ForgeAttachmentPlatform implements IAttachmentPlatform {

    private static final String ROOT_KEY = "abysscore_attachments";

    @Override
    public <T> AttachmentStorage<T> createStorage(ResourceLocation id, T defaultValue, Codec<T> codec) {
        String key = id.toString();

        return new AttachmentStorage<>() {
            @Override
            public T get(Player player) {
                CompoundTag root = player.getPersistentData().getCompound(ROOT_KEY);
                if (!root.contains(key)) return defaultValue;
                return AttachmentNbt.decode(codec, root.getCompound(key), defaultValue, id);
            }

            @Override
            public void set(Player player, T value) {
                CompoundTag persistent = player.getPersistentData();
                CompoundTag root = persistent.getCompound(ROOT_KEY);   // a new empty tag if missing
                root.put(key, AttachmentNbt.encode(codec, value, id));
                persistent.put(ROOT_KEY, root);
            }
        };
    }
}
