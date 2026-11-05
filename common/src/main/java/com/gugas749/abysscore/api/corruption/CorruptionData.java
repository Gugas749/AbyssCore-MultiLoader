package com.gugas749.abysscore.api.corruption;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A player's corruption. Immutable — AbyssCorruption swaps in a new one on every change.
 *
 * @param level    0–100, what the HUD shows and what effects should scale with
 * @param progress ticks accumulated towards the next level (see AbyssCorruption.addProgress)
 *
 * Why a record and not a mutable class like AbyssItems had? AbyssSyncedAttachment syncs on
 * set() — if code could change the object in place, nothing would notice and the client would
 * never be told. With an immutable record, the ONLY way to change it is through the API.
 */
public record CorruptionData(int level, int progress) {

    public static final int MAX = 100;
    public static final CorruptionData NONE = new CorruptionData(0, 0);

    public CorruptionData {
        level    = Math.max(0, Math.min(MAX, level));
        progress = Math.max(0, progress);
    }

    /** Field names match AbyssItems' old attachment, so the saved NBT looks the same. */
    public static final Codec<CorruptionData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("corruption").forGetter(CorruptionData::level),
            Codec.INT.fieldOf("tick_counter").forGetter(CorruptionData::progress)
    ).apply(inst, CorruptionData::new));
}
