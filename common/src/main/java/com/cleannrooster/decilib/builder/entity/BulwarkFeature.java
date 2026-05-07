package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.common.BulwarkBrainGoal;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.validation.ValidationResult;

import java.util.Set;


public final class BulwarkFeature implements BehaviorFeature {

    @Override
    public String typeId() { return "bulwark"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var activationRange = config.getDouble("activation_range", 8.0);
        if (activationRange <= 0) {
            result.error("Feature 'bulwark' in profile '" + profile.id()
                    + "': activation_range must be > 0 (got " + activationRange + ")");
        }

        var deactivationRange = config.getDouble("deactivation_range", activationRange * 0.7);
        if (deactivationRange <= 0) {
            result.error("Feature 'bulwark' in profile '" + profile.id()
                    + "': deactivation_range must be > 0 (got " + deactivationRange + ")");
        }
        if (deactivationRange >= activationRange) {
            result.warn("Feature 'bulwark' in profile '" + profile.id()
                    + "': deactivation_range >= activation_range may cause rapid toggling");
        }

        var reflectCoeff = config.getDouble("reflect_coeff", 0.0);
        if (reflectCoeff < 0) {
            result.error("Feature 'bulwark' in profile '" + profile.id()
                    + "': reflect_coeff must be >= 0 (got " + reflectCoeff + ")");
        }

        var stateName = config.getString("state", "APPROACHING");
        try {
            MobState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            result.error("Feature 'bulwark' in profile '" + profile.id()
                    + "': unknown state '" + stateName + "'");
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var abilityId         = config.getString("ability_id",         "bulwark");
        var cooldown          = config.getInt("cooldown_ticks",        20);
        var activationRange   = config.getDouble("activation_range",   8.0);
        var deactivationRange = config.getDouble("deactivation_range", activationRange * 0.7);
        var reflectCoeff      = (float) config.getDouble("reflect_coeff", 0.0);
        var approachSpeed     = config.getDouble("approach_speed",     1.2);
        var state             = MobState.valueOf(config.getString("state", "APPROACHING"));
        var priority          = config.getInt("priority",              40);
        var hooks             = config.hooks();

        var onStart  = ParticleStyle.fromString(config.getString("on_start_particles",  "none"));
        var onStop   = ParticleStyle.fromString(config.getString("on_stop_particles",   "none"));

        GoalEffects<DataDrivenMob> effects = GoalEffects.<DataDrivenMob>builder()
                .onStart(   (mob, world) -> onStart.spawn(mob, world, 0f, 0f))
                .onComplete((mob, world) -> onStop.spawn(mob, world, 0f, 0f))
                .build();
        if (hooks != null) effects = GoalEffects.compose(effects, hooks.toGoalEffects());

        var goal = new BulwarkBrainGoal<DataDrivenMob>(
                abilityId, cooldown,
                activationRange, deactivationRange,
                reflectCoeff, approachSpeed)
                .withEffects(effects);

        composer.addGoal(goal, Set.of(state), priority);
    }
}
