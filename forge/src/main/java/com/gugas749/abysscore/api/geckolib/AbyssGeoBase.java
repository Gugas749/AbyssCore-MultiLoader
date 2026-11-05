package com.gugas749.abysscore.api.geckolib;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

// GeckoLib 4.x on 1.20.1 keeps these classes under software.bernie.geckolib.core.*
public interface AbyssGeoBase extends GeoAnimatable {

    default AnimatableInstanceCache createCache() {
        return GeckoLibUtil.createInstanceCache(this);
    }
}
