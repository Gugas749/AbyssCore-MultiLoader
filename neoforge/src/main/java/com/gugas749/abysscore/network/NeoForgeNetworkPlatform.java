package com.gugas749.abysscore.network;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.api.network.AbyssChannel;
import com.gugas749.abysscore.api.network.AbyssPacketCodec;
import com.gugas749.abysscore.api.network.AbyssPacketContext;
import com.gugas749.abysscore.api.network.AbyssPacketRegistrar;
import com.gugas749.abysscore.platform.services.INetworkPlatform;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * NeoForge 1.21.1 implementation of the AbyssCore network API.
 * Registered via neoforge/src/main/resources/META-INF/services/...INetworkPlatform.
 *
 * NeoForge wants every packet to be a CustomPacketPayload with a Type (an id). Our packets
 * are plain records (so Forge 1.20.1 can use them too), so each one is put inside a
 * {@link Wrapped} payload that carries the record and its Type.
 *
 * NeoForge registers payloads in RegisterPayloadHandlersEvent (mod event bus), which fires
 * AFTER mod construction. So createChannel() only REMEMBERS the packets, and registers
 * them when that event fires.
 */
public class NeoForgeNetworkPlatform implements INetworkPlatform {

    /** The payload NeoForge actually sends: our record + its Type. */
    public record Wrapped<T>(Type<Wrapped<T>> type, T packet) implements CustomPacketPayload {}

    @Override
    public AbyssChannel createChannel(String modId, String channelName, String version,
                                      Consumer<AbyssPacketRegistrar> register) {
        // channelName isn't needed here: NeoForge identifies each packet by modid:packetName
        Channel channel = new Channel(modId, version);
        register.accept(channel);   // collect the packets now

        // The mod's own event bus. ModList knows every mod container while mods are constructed.
        IEventBus modBus = ModList.get().getModContainerById(modId)
                .map(ModContainer::getEventBus)
                .orElseThrow(() -> new IllegalStateException("createChannel(\"" + modId + "\") called for a mod that isn't loaded"));
        modBus.addListener(RegisterPayloadHandlersEvent.class, channel::registerAll);
        return channel;
    }

    private static final class Channel implements AbyssChannel, AbyssPacketRegistrar {

        private final String modId;
        private final String version;
        private final List<Consumer<PayloadRegistrar>> registrations = new ArrayList<>();
        private final Map<Class<?>, CustomPacketPayload.Type<?>> types = new HashMap<>();

        Channel(String modId, String version) {
            this.modId = modId;
            this.version = version;
        }

        // ── Registration ──────────────────────────────────────────────────────

        @Override
        public <T> void s2c(String name, Class<T> type, AbyssPacketCodec<T> codec,
                            Supplier<BiConsumer<T, AbyssPacketContext>> clientHandler) {
            CustomPacketPayload.Type<Wrapped<T>> payloadType = newType(name, type);
            registrations.add(registrar -> {
                // Only resolve the client handler on the client (it uses client-only classes)
                BiConsumer<T, AbyssPacketContext> handler = FMLEnvironment.dist == Dist.CLIENT
                        ? clientHandler.get()
                        : (pkt, ctx) -> {};
                registrar.playToClient(payloadType, streamCodec(payloadType, codec),
                        (wrapped, ctx) -> handler.accept(wrapped.packet(), new AbyssNeoForgePacketContext(ctx)));
            });
        }

        @Override
        public <T> void c2s(String name, Class<T> type, AbyssPacketCodec<T> codec,
                            BiConsumer<T, AbyssPacketContext> serverHandler) {
            CustomPacketPayload.Type<Wrapped<T>> payloadType = newType(name, type);
            registrations.add(registrar -> registrar.playToServer(payloadType, streamCodec(payloadType, codec),
                    (wrapped, ctx) -> serverHandler.accept(wrapped.packet(), new AbyssNeoForgePacketContext(ctx))));
        }

        private <T> CustomPacketPayload.Type<Wrapped<T>> newType(String name, Class<T> packetClass) {
            CustomPacketPayload.Type<Wrapped<T>> payloadType = new CustomPacketPayload.Type<>(Constants.id(modId, name));
            if (types.put(packetClass, payloadType) != null) {
                throw new IllegalStateException(packetClass.getName() + " registered twice in channel " + modId);
            }
            return payloadType;
        }

        /** AbyssPacketCodec (works on any FriendlyByteBuf) → the StreamCodec NeoForge wants. */
        private static <T> StreamCodec<RegistryFriendlyByteBuf, Wrapped<T>> streamCodec(CustomPacketPayload.Type<Wrapped<T>> type,
                                                                                         AbyssPacketCodec<T> codec) {
            return StreamCodec.of(
                    (buf, wrapped) -> codec.encoder().accept(buf, wrapped.packet()),
                    buf -> new Wrapped<>(type, codec.decoder().apply(buf)));
        }

        void registerAll(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar(version);
            registrations.forEach(r -> r.accept(registrar));
        }

        // ── Sending ───────────────────────────────────────────────────────────

        @SuppressWarnings({"unchecked", "rawtypes"})
        private CustomPacketPayload wrap(Object packet) {
            CustomPacketPayload.Type type = types.get(packet.getClass());
            if (type == null) {
                throw new IllegalArgumentException(packet.getClass().getName() + " is not registered in channel " + modId);
            }
            return new Wrapped(type, packet);
        }

        @Override
        public void sendToPlayer(ServerPlayer player, Object packet) {
            PacketDistributor.sendToPlayer(player, wrap(packet));
        }

        @Override
        public void sendToAll(Object packet) {
            PacketDistributor.sendToAllPlayers(wrap(packet));
        }

        @Override
        public void sendToTracking(Entity entity, Object packet) {
            PacketDistributor.sendToPlayersTrackingEntity(entity, wrap(packet));
        }

        @Override
        public void sendToTrackingAndSelf(Entity entity, Object packet) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, wrap(packet));
        }

        @Override
        public void sendToServer(Object packet) {
            PacketDistributor.sendToServer(wrap(packet));
        }
    }
}
