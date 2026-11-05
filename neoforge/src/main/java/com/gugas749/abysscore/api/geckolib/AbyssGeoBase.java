package com.gugas749.abysscore.api.geckolib;

import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public interface AbyssGeoBase extends GeoAnimatable {

    default AnimatableInstanceCache createCache() {
        return GeckoLibUtil.createInstanceCache(this);
    }
}
