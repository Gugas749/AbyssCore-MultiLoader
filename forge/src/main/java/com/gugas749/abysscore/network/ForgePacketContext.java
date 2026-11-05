package com.gugas749.abysscore.network;

import com.gugas749.abysscore.api.network.AbyssPacketContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

/**
 * Wraps Forge 1.20.1's NetworkEvent.Context as AbyssPacketContext, so the common packet
 * handlers work unchanged on Forge.
 *
 *   C2S (client → server): player() = ctx.getSender(), the ServerPlayer who sent it.
 *   S2C (server → client): getSender() is null on the client, so player() returns the local
 *                          player instead (same as NeoForge's IPayloadContext.player()).
 */
public record ForgePacketContext(NetworkEvent.Context ctx) implements AbyssPacketContext {

    @Override
    public void enqueueWork(Runnable runnable) {
        ctx.enqueueWork(runnable);
    }

    @Override
    public Player player() {
        if (ctx.getDirection().getReceptionSide().isServer()) {
            return ctx.getSender();
        }
        return ClientSide.localPlayer();
    }

    /**
     * A separate class so Minecraft (client-only) is only loaded when this runs, which is
     * only ever on the client. The JVM loads a class the first time code that uses it RUNS.
     */
    private static final class ClientSide {
        static Player localPlayer() {
            return Minecraft.getInstance().player;
        }
    }
}
