package com.gugas749.abysscore.api.geckolib;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;

public abstract class AbyssGeoEntity  extends Entity implements AbyssGeoBase, GeoEntity {

    /*
     *           EXAMPLE ENTITY
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
     *       public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
     *           // just the renderer
     *       }
     *
     *       @Override
     *       protected void defineSynchedData(SynchedEntityData.Builder builder) {
     *           // entity-specific synced data if needed
     *       }
     *
     *       @Override
     *       protected void readAdditionalSaveData(CompoundTag tag) {}
     *
     *       @Override
     *       protected void addAdditionalSaveData(CompoundTag tag) {}
     *   }
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
