package com.cleannrooster.decilib.builder.registry;

import com.cleannrooster.decilib.builder.archetype.AmbusherArchetype;
import com.cleannrooster.decilib.builder.archetype.BruiserArchetype;
import com.cleannrooster.decilib.builder.archetype.SkirmisherArchetype;
import com.cleannrooster.decilib.builder.entity.AmbushAttackFeature;
import com.cleannrooster.decilib.builder.entity.AmbushFeature;
import com.cleannrooster.decilib.builder.entity.BraceFeature;
import com.cleannrooster.decilib.builder.entity.BulwarkFeature;
import com.cleannrooster.decilib.builder.entity.ChargeFeature;
import com.cleannrooster.decilib.builder.entity.ChaseFeature;
import com.cleannrooster.decilib.builder.entity.FrenzyFeature;
import com.cleannrooster.decilib.builder.entity.GapcloseFeature;
import com.cleannrooster.decilib.builder.entity.IdleFeature;
import com.cleannrooster.decilib.builder.entity.LeaveHazardFeature;
import com.cleannrooster.decilib.builder.entity.MeleeFeature;
import com.cleannrooster.decilib.builder.entity.RangedAttackFeature;
import com.cleannrooster.decilib.builder.entity.RepositionFeature;
import com.cleannrooster.decilib.builder.entity.RangePunishFeature;
import com.cleannrooster.decilib.builder.entity.ResetPunishmentFeature;
import com.cleannrooster.decilib.builder.entity.ShockwaveFeature;
import com.cleannrooster.decilib.builder.entity.SweepFeature;
import com.cleannrooster.decilib.builder.entity.ZoneDenialFeature;
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
        FeatureRegistry.register(new FrenzyFeature());
        FeatureRegistry.register(new BraceFeature());
        FeatureRegistry.register(new ChaseFeature());
        FeatureRegistry.register(new GapcloseFeature());
        FeatureRegistry.register(new ZoneDenialFeature());
        FeatureRegistry.register(new ResetPunishmentFeature());
        FeatureRegistry.register(new RangePunishFeature());
        FeatureRegistry.register(new BulwarkFeature());
        FeatureRegistry.register(new RangedAttackFeature());
        FeatureRegistry.register(new RepositionFeature());
        FeatureRegistry.register(new LeaveHazardFeature());

        PresetRegistry.register(new BurrowerPreset());
    }
}
