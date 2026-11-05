package com.gugas749.abysscore.platform;

import com.gugas749.abysscore.platform.services.IPlatformHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Forge";
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

    /** 1.20.1: computeIfAbsent takes the loader, the constructor and the name directly. */
    @Override
    public <T extends SavedData> T getSavedData(ServerLevel level, String name, Supplier<T> factory,
                                                Function<CompoundTag, T> loader) {
        return level.getDataStorage().computeIfAbsent(loader, factory, name);
    }

    /** 1.20.1: getEffect() is the MobEffect itself; look its id up in the registry. */
    @Override
    public ResourceLocation getEffectId(MobEffectInstance effect) {
        return BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect());
    }
}
