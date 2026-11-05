package com.gugas749.abysscore.network;

import com.gugas749.abysscore.api.network.AbyssPacketContext;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Wraps NeoForge's IPayloadContext to implement AbyssPacketContext.
 *
 * Used in PacketHandler when registering handlers: instead of passing the raw
 * IPayloadContext to the common handler methods, we wrap it and pass the wrapper.
 * This way common handler classes (KeyPressHandler, MenuPacketHandlers, etc.) only
 * import AbyssPacketContext — a common-module interface — and compile without NeoForge.
 *
 * Why a record?  Records are immutable and give us equals/hashCode/toString for free.
 * The wrapped IPayloadContext is provided by NeoForge and lives exactly as long as the
 * packet dispatch, so we never keep a stale reference.
 */
public record AbyssNeoForgePacketContext(IPayloadContext ctx) implements AbyssPacketContext {

    @Override
    public void enqueueWork(Runnable runnable) {
        ctx.enqueueWork(runnable);
    }

    @Override
    public Player player() {
        return ctx.player();
    }
}
