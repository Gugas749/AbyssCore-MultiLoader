package com.gugas749.abysscore.api.attachment;

import com.gugas749.abysscore.Constants;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/**
 * Value ⇄ NBT with a Codec, used for the sync packet and by Forge's storage.
 *
 * The value is wrapped as {"v": <encoded>} because a codec can produce ANY tag type
 * (Codec.INT gives an IntTag, a record codec a CompoundTag...) and a packet / the persistent
 * data want one consistent shape. Same format as the forge-1.20.1 branch, so old Forge saves load.
 *
 * resultOrPartial(...) is used instead of getOrThrow(...) because getOrThrow has different
 * parameters in 1.20.1's and 1.21.1's DataFixerUpper; resultOrPartial is the same in both.
 */
public final class AttachmentNbt {

    private static final String VALUE_KEY = "v";

    private AttachmentNbt() {}

    public static <T> CompoundTag encode(Codec<T> codec, T value, ResourceLocation id) {
        CompoundTag wrapper = new CompoundTag();
        codec.encodeStart(NbtOps.INSTANCE, value)
                .resultOrPartial(err -> Constants.LOG.error("[AbyssCore] Failed to encode attachment {}: {}", id, err))
                .ifPresent(tag -> wrapper.put(VALUE_KEY, tag));
        return wrapper;
    }

    /** Returns {@code fallback} if the tag is missing or can't be read. */
    public static <T> T decode(Codec<T> codec, CompoundTag wrapper, T fallback, ResourceLocation id) {
        Tag raw = wrapper.get(VALUE_KEY);
        if (raw == null) return fallback;
        return codec.parse(NbtOps.INSTANCE, raw)
                .resultOrPartial(err -> Constants.LOG.error("[AbyssCore] Failed to decode attachment {}: {}", id, err))
                .orElse(fallback);
    }
}
