package com.gugas749.abysscore.network;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.api.network.AbyssChannel;
import com.gugas749.abysscore.api.network.AbyssPacketCodec;
import com.gugas749.abysscore.api.network.AbyssPacketContext;
import com.gugas749.abysscore.api.network.AbyssPacketRegistrar;
import com.gugas749.abysscore.platform.services.INetworkPlatform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Forge 1.20.1 implementation of the AbyssCore network API (moved from the forge-1.20.1
 * branch's AbyssNetworkChannel + AbyssPacketHandler).
 * Registered via forge/src/main/resources/META-INF/services/...INetworkPlatform.
 *
 * One SimpleChannel per AbyssChannel, named modid:channelName (usually modid:main). Packets are identified by an INDEX
 * (0, 1, 2...) given in registration order — that's why the order in PacketHandler matters
 * and the packet names are ignored here.
 */
public class ForgeNetworkPlatform implements INetworkPlatform {

    @Override
    public AbyssChannel createChannel(String modId, String channelName, String version,
                                      Consumer<AbyssPacketRegistrar> register) {
        SimpleChannel simpleChannel = NetworkRegistry.newSimpleChannel(
                Constants.id(modId, channelName),
                () -> version,
                version::equals,   // versions the client accepts from the server
                version::equals);  // versions the server accepts from the client
        Channel channel = new Channel(simpleChannel);
        register.accept(channel);   // unlike NeoForge, Forge registers packets right away
        return channel;
    }

    private static final class Channel implements AbyssChannel, AbyssPacketRegistrar {

        private final SimpleChannel channel;
        private int nextId = 0;

        Channel(SimpleChannel channel) {
            this.channel = channel;
        }

        @Override
        public <T> void s2c(String name, Class<T> type, AbyssPacketCodec<T> codec,
                            Supplier<BiConsumer<T, AbyssPacketContext>> clientHandler) {
            // On a dedicated server the client handler classes don't exist, so we never call
            // clientHandler.get() there. The packet is still registered (its index must exist
            // on both sides), it just gets a do-nothing handler that is never used.
            BiConsumer<T, AbyssPacketContext> handler = FMLEnvironment.dist == Dist.CLIENT
                    ? clientHandler.get()
                    : (pkt, ctx) -> {};
            register(type, codec, handler, NetworkDirection.PLAY_TO_CLIENT);
        }

        @Override
        public <T> void c2s(String name, Class<T> type, AbyssPacketCodec<T> codec,
                            BiConsumer<T, AbyssPacketContext> serverHandler) {
            register(type, codec, serverHandler, NetworkDirection.PLAY_TO_SERVER);
        }

        private <T> void register(Class<T> type, AbyssPacketCodec<T> codec,
                                  BiConsumer<T, AbyssPacketContext> handler, NetworkDirection direction) {
            channel.messageBuilder(type, nextId++, direction)
                    // Forge's encoder is (packet, buf); our codec is (buf, packet) — swap here
                    .encoder((pkt, buf) -> codec.encoder().accept(buf, pkt))
                    .decoder(buf -> codec.decoder().apply(buf))
                    .consumerNetworkThread((pkt, ctxSupplier) -> {
                        NetworkEvent.Context ctx = ctxSupplier.get();
                        // Handlers call ctx.enqueueWork(...) themselves, exactly like on NeoForge
                        handler.accept(pkt, new ForgePacketContext(ctx));
                        ctx.setPacketHandled(true);
                    })
                    .add();
        }

        // ── Sending ───────────────────────────────────────────────────────────

        @Override
        public void sendToPlayer(ServerPlayer player, Object packet) {
            channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }

        @Override
        public void sendToAll(Object packet) {
            channel.send(PacketDistributor.ALL.noArg(), packet);
        }

        @Override
        public void sendToTracking(Entity entity, Object packet) {
            channel.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
        }

        @Override
        public void sendToTrackingAndSelf(Entity entity, Object packet) {
            channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
        }

        @Override
        public void sendToServer(Object packet) {
            channel.sendToServer(packet);
        }
    }
}
