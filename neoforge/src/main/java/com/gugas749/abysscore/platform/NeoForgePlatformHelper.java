package com.gugas749.abysscore.platform;

import com.gugas749.abysscore.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    /** 1.21.1: computeIfAbsent takes a SavedData.Factory(constructor, loader(tag, registries), dataFixType). */
    @Override
    public <T extends SavedData> T getSavedData(ServerLevel level, String name, Supplier<T> factory,
                                                Function<CompoundTag, T> loader) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(factory, (tag, registries) -> loader.apply(tag), null), name);
    }

    /** 1.21.1: getEffect() is a Holder; its key holds the id. */
    @Override
    public ResourceLocation getEffectId(MobEffectInstance effect) {
        return effect.getEffect().unwrapKey().map(ResourceKey::location).orElse(null);
    }
}
