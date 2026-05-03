package com.cleannrooster.decilib.builder.feature;

import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.validation.ValidationResult;

/**
 * Pluggable behavior module that adds goals and transitions to the
 * {@link BehaviorComposer} for a specific capability (ambush, enrage,
 * ranged attack, etc.).
 *
 * <p>Features are registered in {@link com.cleannrooster.decilib.builder.registry.FeatureRegistry}
 * by type ID. Modpack profiles reference them by that ID in the {@code features}
 * array.
 *
 * <p>Implementation notes:
 * <ul>
 *   <li>{@link #validate} is called before {@link #apply}. Add errors for
 *       invalid config values and warnings for suspicious-but-valid ones.
 *   <li>{@link #apply} is only called when validation passes. Do not repeat
 *       validation logic here.
 * </ul>
 */
public interface BehaviorFeature {

    String typeId();


    void validate(FeatureConfig config, MobProfile profile, ValidationResult result);


    void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning);
}
