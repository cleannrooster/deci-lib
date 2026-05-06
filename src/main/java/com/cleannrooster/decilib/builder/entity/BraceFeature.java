package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.common.BraceBrainGoal;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.validation.ValidationResult;

import java.util.Set;


public final class BraceFeature implements BehaviorFeature {

    @Override
    public String typeId() { return "brace"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var reduction = config.getDouble("damage_reduction", 0.5);
        if (reduction <= 0 || reduction >= 1) {
            result.error("Feature 'brace' in profile '" + profile.id()
                    + "': damage_reduction must be in (0, 1) (got " + reduction + ")");
        }

        var duration = config.getInt("duration_ticks", 30);
        if (duration < 1) {
            result.error("Feature 'brace' in profile '" + profile.id()
                    + "': duration_ticks must be >= 1 (got " + duration + ")");
        }

        var cooldown = config.getInt("cooldown_ticks", 100);
        if (cooldown < 1) {
            result.error("Feature 'brace' in profile '" + profile.id()
                    + "': cooldown_ticks must be >= 1 (got " + cooldown + ")");
        }

        var conditionMode = config.getString("condition_mode", "any");
        if (!conditionMode.equals("any") && !conditionMode.equals("all")) {
            result.error("Feature 'brace' in profile '" + profile.id()
                    + "': condition_mode must be 'any' or 'all' (got '" + conditionMode + "')");
        }

        boolean hasCondition = config.getBoolean("check_health_threshold", false)
                || config.getBoolean("check_ranged_hit", true)
                || config.getBoolean("check_burst_damage", false);
        if (!hasCondition) {
            result.warn("Feature 'brace' in profile '" + profile.id()
                    + "': no conditions enabled — brace will never trigger");
        }

        var stateName = config.getString("state", "APPROACHING");
        try {
            MobState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            result.error("Feature 'brace' in profile '" + profile.id()
                    + "': unknown state '" + stateName + "'");
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var abilityId    = config.getString("ability_id",          "brace");
        var cooldown     = config.getInt("cooldown_ticks",         100);
        var duration     = config.getInt("duration_ticks",         30);
        var reduction    = (float) config.getDouble("damage_reduction", 0.5);
        var condMode     = config.getString("condition_mode",      "any");
        var requireAll   = condMode.equals("all");
        var state        = MobState.valueOf(config.getString("state", "APPROACHING"));
        var priority     = config.getInt("priority",               50);
        var hooks        = config.hooks();

        var checkHealth  = config.getBoolean("check_health_threshold",    false);
        var healthPct    = (float) config.getDouble("health_threshold_pct", 0.5);
        var checkRanged  = config.getBoolean("check_ranged_hit",           true);
        var rangedWindow = config.getInt("ranged_hit_window_ticks",        40);
        var rangedMinDist = config.getDouble("ranged_hit_min_distance",    8.0);
        var checkBurst   = config.getBoolean("check_burst_damage",         false);
        var burstThresh  = (float) config.getDouble("burst_damage_threshold", 10.0);

        var onStart    = ParticleStyle.fromString(config.getString("on_start_particles",    "none"));
        var onComplete = ParticleStyle.fromString(config.getString("on_complete_particles", "none"));
        var hooks2     = hooks;

        GoalEffects<DataDrivenMob> effects = GoalEffects.<DataDrivenMob>builder()
                .onStart(   (mob, world) -> onStart.spawn(mob, world, 0f, 0f))
                .onComplete((mob, world) -> onComplete.spawn(mob, world, 0f, 0f))
                .build();
        if (hooks2 != null) effects = GoalEffects.compose(effects, hooks2.toGoalEffects());

        var goal = new BraceBrainGoal<DataDrivenMob>(
                abilityId, cooldown, duration, reduction,
                checkHealth, healthPct,
                checkRanged, rangedWindow, rangedMinDist,
                checkBurst, burstThresh, requireAll)
                .withEffects(effects);

        composer.addGoal(goal, Set.of(state), priority);
    }
}
