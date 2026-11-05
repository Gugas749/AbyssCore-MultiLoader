package com.gugas749.abysscore.platform;

import com.gugas749.abysscore.api.attachment.AttachmentStorage;
import com.gugas749.abysscore.platform.services.IAttachmentPlatform;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * NeoForge 1.21.1: an AbyssSyncedAttachment is saved as a NeoForge DATA ATTACHMENT
 * (AttachmentType + serialize(codec) → NeoForge saves it with the player).
 * Registered via neoforge/src/main/resources/META-INF/services/...IAttachmentPlatform.
 *
 * Attachment types are registry entries, so they need a DeferredRegister for their namespace,
 * registered on THAT mod's event bus. We make one per namespace the first time it's used
 * (abysscore:corruption → abysscore's, mymod:mana → mymod's). The ids are the same as before
 * (abysscore:corruption), so data saved by the old NeoForge-only version still loads.
 */
public class NeoForgeAttachmentPlatform implements IAttachmentPlatform {

    private static final Map<String, DeferredRegister<AttachmentType<?>>> REGISTERS = new HashMap<>();

    @Override
    public <T> AttachmentStorage<T> createStorage(ResourceLocation id, T defaultValue, Codec<T> codec) {
        DeferredRegister<AttachmentType<?>> register =
                REGISTERS.computeIfAbsent(id.getNamespace(), NeoForgeAttachmentPlatform::newRegister);

        Supplier<AttachmentType<T>> type = register.register(id.getPath(),
                () -> AttachmentType.builder(() -> defaultValue).serialize(codec).build());

        return new AttachmentStorage<>() {
            @Override
            public T get(Player player) {
                return player.getData(type.get());
            }

            @Override
            public void set(Player player, T value) {
                player.setData(type.get(), value);
            }
        };
    }

    private static DeferredRegister<AttachmentType<?>> newRegister(String namespace) {
        DeferredRegister<AttachmentType<?>> register =
                DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, namespace);
        IEventBus modBus = ModList.get().getModContainerById(namespace)
                .map(ModContainer::getEventBus)
                .orElseThrow(() -> new IllegalStateException(
                        "AbyssSyncedAttachment namespace \"" + namespace + "\" must be a loaded mod id"));
        register.register(modBus);
        return register;
    }
}
