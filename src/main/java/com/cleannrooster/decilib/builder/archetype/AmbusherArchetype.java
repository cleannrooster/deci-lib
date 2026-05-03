package com.cleannrooster.decilib.builder.archetype;

import com.cleannrooster.decilib.builder.MobStance;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.tuning.TuningResolver;

import java.util.Set;

public final class AmbusherArchetype implements Archetype {

    @Override
    public String id() { return "ambusher"; }

    @Override
    public Set<String> requiredFeatures() { return Set.of("ambush"); }

    @Override
    public Set<String> incompatibleFeatures() { return Set.of(); }

    @Override
    public Set<MobState> supportedStates() {
        return Set.of(MobState.HIDDEN, MobState.ACTIVE, MobState.REHIDING);
    }

    @Override
    public MobStance initialStance() { return MobStance.PASSIVE; }

    @Override
    public MobState initialState()   { return MobState.HIDDEN; }

    @Override
    public BaseStats baseStats(TuningProfile tuning) {
        return new BaseStats(
                TuningResolver.maxHealth(tuning.health()),
                TuningResolver.attackDamage(tuning.damage()),
                TuningResolver.movementSpeed(tuning.speed()),
                TuningResolver.followRange(tuning.detection()),
                0.0,
                2.0
        );
    }

    @Override
    public void apply(BehaviorComposer composer, TuningProfile tuning) {}
}
