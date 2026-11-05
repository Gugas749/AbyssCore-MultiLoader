package com.gugas749.abysscore.platform;

import com.gugas749.abysscore.Constants;
import com.gugas749.abysscore.platform.services.IAttachmentPlatform;
import com.gugas749.abysscore.platform.services.INetworkPlatform;
import com.gugas749.abysscore.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

public class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    /**
     * Loaded by each loader module via META-INF/services/INetworkPlatform.
     * Provides sendToPlayer / sendToServer so common code has no PacketDistributor imports.
     */
    public static final INetworkPlatform NETWORK = load(INetworkPlatform.class);

    /** Where AbyssSyncedAttachment values are saved: data attachments (NeoForge) / persistent NBT (Forge). */
    public static final IAttachmentPlatform ATTACHMENTS = load(IAttachmentPlatform.class);

    // This code is used to load a service for the current environment. Your implementation of the service must be defined
    // manually by including a text file in META-INF/services named with the fully qualified class name of the service.
    // Inside the file you should write the fully qualified class name of the implementation to load for the platform. For
    // example our file on Forge points to ForgePlatformHelper while Fabric points to FabricPlatformHelper.
    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz).findFirst().orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
