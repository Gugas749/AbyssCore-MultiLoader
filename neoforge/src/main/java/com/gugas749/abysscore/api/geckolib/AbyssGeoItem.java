package com.gugas749.abysscore.api.geckolib;

import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.RenderUtil;

public abstract class AbyssGeoItem extends Item implements AbyssGeoBase, GeoItem {

    /*
    *           EXAMPLE ITEM REGISTRATION
    *
    *   public class RingBoxItem extends AbyssGeoItem {

            public RingBoxItem() {
                super(new Properties().fireResistant()...);
            }

            @Override
            public void registerControllers(...) {
                // just the animation logic
            }

            @Override
            public void createGeoRenderer(...) {
                // just the renderer
            }
        }
    *
    *
    * */

    private final AnimatableInstanceCache cache = createCache();

    public AbyssGeoItem(Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object o) {
        return RenderUtil.getCurrentTick();
    }

    @Override
    public abstract void registerControllers(AnimatableManager.ControllerRegistrar controllers);
}
