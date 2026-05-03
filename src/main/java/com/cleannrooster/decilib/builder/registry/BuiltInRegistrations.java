package com.cleannrooster.decilib.builder.registry;

import com.cleannrooster.decilib.builder.archetype.AmbusherArchetype;
import com.cleannrooster.decilib.builder.archetype.BruiserArchetype;
import com.cleannrooster.decilib.builder.archetype.SkirmisherArchetype;
import com.cleannrooster.decilib.builder.entity.AmbushAttackFeature;
import com.cleannrooster.decilib.builder.entity.AmbushFeature;
import com.cleannrooster.decilib.builder.entity.ChargeFeature;
import com.cleannrooster.decilib.builder.entity.IdleFeature;
import com.cleannrooster.decilib.builder.entity.MeleeFeature;
import com.cleannrooster.decilib.builder.entity.ShockwaveFeature;
import com.cleannrooster.decilib.builder.entity.SweepFeature;
import com.cleannrooster.decilib.builder.preset.BurrowerPreset;


public final class BuiltInRegistrations {

    private static boolean registered = false;

    private BuiltInRegistrations() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        ArchetypeRegistry.register(new AmbusherArchetype());
        ArchetypeRegistry.register(new BruiserArchetype());
        ArchetypeRegistry.register(new SkirmisherArchetype());

        FeatureRegistry.register(new AmbushAttackFeature());
        FeatureRegistry.register(new AmbushFeature());
        FeatureRegistry.register(new IdleFeature());
        FeatureRegistry.register(new MeleeFeature());
        FeatureRegistry.register(new SweepFeature());
        FeatureRegistry.register(new ShockwaveFeature());
        FeatureRegistry.register(new ChargeFeature());

        PresetRegistry.register(new BurrowerPreset());
    }
}
