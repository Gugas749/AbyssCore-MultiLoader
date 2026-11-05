package com.gugas749.abysscore.api.damages;

import com.gugas749.abysscore.Constants;
import net.minecraft.core.registries.Registries;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;

public class AbyssDamageTypes {
    public static final ResourceKey<DamageType>
            CORRUPTION_DAMAGE = key("corruption_damage"),
            TEST_DAMAGE = key("test_damage");

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Constants.asResource(name));
    }

    /** Datagen: AbyssDamageTypes.bootstrap(ctx::register) — see DamageTypeBuilder.register. */
    public static void bootstrap(BiConsumer<ResourceKey<DamageType>, DamageType> ctx) {
        new DamageTypeBuilder(CORRUPTION_DAMAGE).scaling(DamageScaling.ALWAYS).exhaustion(0.1f).register(ctx);
        new DamageTypeBuilder(TEST_DAMAGE).scaling(DamageScaling.ALWAYS).register(ctx);
    }
}
