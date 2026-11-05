package com.gugas749.abysscore.api.geckolib;

import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.RenderUtils;

public abstract class AbyssGeoItem extends Item implements AbyssGeoBase, GeoItem {

    /*
    *           EXAMPLE ITEM (Forge 1.20.1)
    *
    *   public class RingBoxItem extends AbyssGeoItem {

            public RingBoxItem() {
                super(new Properties().fireResistant());
            }

            @Override
            public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
                // just the animation logic
            }

            // On Forge 1.20.1 the renderer is attached with initializeClient
            // (on NeoForge 1.21.1 this was RegisterClientExtensionsEvent)
            @Override
            public void initializeClient(Consumer<IClientItemExtensions> consumer) {
                consumer.accept(new IClientItemExtensions() {
                    private RingBoxRenderer renderer;

                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        if (renderer == null) renderer = new RingBoxRenderer();
                        return renderer;
                    }
                });
            }
        }
    *
    * */

    private final AnimatableInstanceCache cache = createCache();

    public AbyssGeoItem(Properties properties) {
        super(properties);
        // GeckoLib 4 on 1.20.1: the static method lives on SingletonGeoAnimatable.
        // Static interface methods are NOT inherited in Java, so GeoItem.registerSyncedAnimatable won't compile.
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object o) {
        return RenderUtils.getCurrentTick();
    }

    @Override
    public abstract void registerControllers(AnimatableManager.ControllerRegistrar controllers);
}
