package com.gugas749.abysscore.api.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * How a packet is written to / read from the network. Works on Forge 1.20.1 and NeoForge
 * 1.21.1 (FriendlyByteBuf exists in both; StreamCodec only exists in 1.20.5+).
 *
 * A codec is just two functions:
 *   - encoder: writes the packet's fields into the buffer
 *   - decoder: reads them back in the SAME order and builds a new packet
 *
 * The argument order of {@link #of} matches {@code StreamCodec.of(encoder, decoder)},
 * so old lambdas like {@code (buf, pkt) -> buf.writeInt(pkt.slot())} work unchanged.
 */
public record AbyssPacketCodec<T>(
        BiConsumer<FriendlyByteBuf, T> encoder,
        Function<FriendlyByteBuf, T> decoder
) {

    public static <T> AbyssPacketCodec<T> of(BiConsumer<FriendlyByteBuf, T> encoder,
                                             Function<FriendlyByteBuf, T> decoder) {
        return new AbyssPacketCodec<>(encoder, decoder);
    }

    /** For packets with no fields: writes nothing, always returns the same instance. */
    public static <T> AbyssPacketCodec<T> unit(T instance) {
        return new AbyssPacketCodec<>((buf, pkt) -> {}, buf -> instance);
    }
}
