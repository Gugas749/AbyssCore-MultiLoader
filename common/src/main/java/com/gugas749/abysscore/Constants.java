package com.gugas749.abysscore;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {

    public static final String MOD_ID = "abysscore";
    public static final String MOD_NAME = "AbyssCore";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    public static ResourceLocation asResource(String path) {
        return id(MOD_ID, path);
    }

    /**
     * namespace:path → ResourceLocation, on BOTH versions. 1.21.1 only has
     * ResourceLocation.fromNamespaceAndPath (the constructor is private) and 1.20.1 only has
     * the constructor, but tryBuild exists in both. It returns null for invalid characters,
     * so we turn that into a clear error.
     */
    public static ResourceLocation id(String namespace, String path) {
        ResourceLocation id = ResourceLocation.tryBuild(namespace, path);
        if (id == null) throw new IllegalArgumentException("Invalid resource location: " + namespace + ":" + path);
        return id;
    }
}
