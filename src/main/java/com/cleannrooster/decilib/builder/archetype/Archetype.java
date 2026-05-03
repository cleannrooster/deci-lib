package com.cleannrooster.decilib.builder.archetype;

import com.cleannrooster.decilib.builder.MobStance;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;

import java.util.Set;

public interface Archetype {

    String id();

    Set<String> requiredFeatures();

    Set<String> incompatibleFeatures();

    Set<MobState> supportedStates();

    MobStance initialStance();

    MobState initialState();

    BaseStats baseStats(TuningProfile tuning);

    void apply(BehaviorComposer composer, TuningProfile tuning);
}
