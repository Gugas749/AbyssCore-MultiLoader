package com.gugas749.abysscore.platform.services;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Returns the loader's config directory (e.g. .minecraft/config/).
     * Common code uses this instead of FMLPaths so it compiles without NeoForge/Forge imports.
     */
    Path getConfigDir();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * level.getDataStorage().computeIfAbsent(...) — loads the saved data called {@code name}
     * from the world folder, or creates it with {@code factory} if there's no file yet.
     * The vanilla method changed shape in 1.20.2 (a SavedData.Factory record), so each
     * loader implements this for its own Minecraft version.
     */
    <T extends SavedData> T getSavedData(ServerLevel level, String name, Supplier<T> factory,
                                         Function<CompoundTag, T> loader);

    /**
     * The registry id of an active effect (minecraft:poison), or null if it isn't registered.
     * MobEffectInstance.getEffect() returns a Holder on 1.21.1 but a MobEffect on 1.20.1.
     */
    ResourceLocation getEffectId(MobEffectInstance effect);

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }
}
