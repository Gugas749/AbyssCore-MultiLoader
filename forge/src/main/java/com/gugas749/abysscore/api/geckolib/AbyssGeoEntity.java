package com.gugas749.abysscore.api.geckolib;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

public abstract class AbyssGeoEntity extends Entity implements AbyssGeoBase, GeoEntity {

    /*
     *           EXAMPLE ENTITY (Forge 1.20.1)
     *
     *   public class VortexEntity extends AbyssGeoEntity {
     *
     *       public VortexEntity(EntityType<?> entityType, Level level) {
     *           super(entityType, level);
     *       }
     *
     *       @Override
     *       public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
     *           // just the animation logic
     *       }
     *
     *       @Override
     *       protected void defineSynchedData() {
     *           // 1.20.1: no Builder parameter — use this.entityData.define(...)
     *       }
     *
     *       @Override
     *       protected void readAdditionalSaveData(CompoundTag tag) {}
     *
     *       @Override
     *       protected void addAdditionalSaveData(CompoundTag tag) {}
     *   }
     *
     *   // The renderer is registered on the client with
     *   // EntityRenderersEvent.RegisterRenderers → event.registerEntityRenderer(TYPE, VortexRenderer::new)
     *
     * */

    private final AnimatableInstanceCache cache = createCache();

    public AbyssGeoEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public double getTick(Object o) {
        return tickCount;
    }

    @Override
    public abstract void registerControllers(AnimatableManager.ControllerRegistrar controllers);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
