package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.common.ChaseBrainGoal;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.validation.ValidationResult;
import net.minecraft.entity.mob.MobEntity;

import java.util.Set;


public final class ChaseFeature implements BehaviorFeature {

    @Override
    public String typeId() { return "chase"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var speed = config.getDouble("chase_speed", 1.8);
        if (speed <= 0) {
            result.error("Feature 'chase' in profile '" + profile.id()
                    + "': chase_speed must be > 0 (got " + speed + ")");
        }

        var engageRange = config.getDouble("engage_range", 5.0);
        if (engageRange <= 0) {
            result.error("Feature 'chase' in profile '" + profile.id()
                    + "': engage_range must be > 0 (got " + engageRange + ")");
        }

        var cooldown = config.getInt("cooldown_ticks", 60);
        if (cooldown < 1) {
            result.error("Feature 'chase' in profile '" + profile.id()
                    + "': cooldown_ticks must be >= 1 (got " + cooldown + ")");
        }

        var maxDuration = config.getInt("max_duration_ticks", 100);
        if (maxDuration < 1) {
            result.error("Feature 'chase' in profile '" + profile.id()
                    + "': max_duration_ticks must be >= 1 (got " + maxDuration + ")");
        }

        var stateName = config.getString("state", "APPROACHING");
        try {
            MobState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            result.error("Feature 'chase' in profile '" + profile.id()
                    + "': unknown state '" + stateName + "'");
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var abilityId        = config.getString("ability_id",           "chase");
        var cooldown         = config.getInt("cooldown_ticks",          60);
        var chaseSpeed       = config.getDouble("chase_speed",          1.8);
        var engageRange      = config.getDouble("engage_range",         5.0);
        var idleAttackWindow = config.getInt("idle_attack_window_ticks", 40);
        var maxDuration      = config.getInt("max_duration_ticks",      100);
        var state            = MobState.valueOf(config.getString("state", "APPROACHING"));
        var priority         = config.getInt("priority",                30);
        var hooks            = config.hooks();

        var onStart    = ParticleStyle.fromString(config.getString("on_start_particles",    "none"));
        var onComplete = ParticleStyle.fromString(config.getString("on_complete_particles", "none"));

        GoalEffects<MobEntity> effects = GoalEffects.<MobEntity>builder()
                .onStart(   (mob, world) -> onStart.spawn(mob, world, 0f, 0f))
                .onComplete((mob, world) -> onComplete.spawn(mob, world, 0f, 0f))
                .build();
        if (hooks != null) effects = GoalEffects.compose(effects, hooks.toGoalEffects());

        var goal = new ChaseBrainGoal<MobEntity>(
                abilityId, cooldown, chaseSpeed, engageRange, idleAttackWindow, maxDuration)
                .withEffects(effects);

        composer.addOffensiveGoal(goal, Set.of(state), priority);
    }
}
