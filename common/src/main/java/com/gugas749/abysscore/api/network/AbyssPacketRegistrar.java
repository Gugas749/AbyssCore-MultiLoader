package com.gugas749.abysscore.api.network;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Handed to your registration method by {@link AbyssNetworking#createChannel}. Declare every
 * packet of your mod here, once.
 *
 *   private static void register(AbyssPacketRegistrar r) {
 *       r.s2c("my_sync", MySyncPacket.class, MySyncPacket.CODEC, () -> MyClientHandler::handle);
 *       r.c2s("my_click", MyClickPacket.class, MyClickPacket.CODEC, MyServerHandler::handle);
 *   }
 *
 * ORDER MATTERS on Forge: packets are numbered 0, 1, 2... in registration order and the
 * number is what goes over the wire. Always add new packets at the END.
 *
 * {@code name} is the packet id on NeoForge (modid:name); Forge ignores it.
 */
public interface AbyssPacketRegistrar {

    /**
     * Server → client. The handler is a Supplier so it's only resolved on the CLIENT:
     * client handlers use client-only classes (Minecraft, screens) that don't exist on a
     * dedicated server.
     */
    <T> void s2c(String name, Class<T> type, AbyssPacketCodec<T> codec,
                 Supplier<BiConsumer<T, AbyssPacketContext>> clientHandler);

    /** Client → server. */
    <T> void c2s(String name, Class<T> type, AbyssPacketCodec<T> codec,
                 BiConsumer<T, AbyssPacketContext> serverHandler);
}
