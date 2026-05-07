package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.common.ZoneDenialBrainGoal;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.validation.ValidationResult;
import net.minecraft.entity.mob.MobEntity;

import java.util.Set;


public final class ZoneDenialFeature implements BehaviorFeature {

    @Override
    public String typeId() { return "zone_denial"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var cooldown = config.getInt("cooldown_ticks", 120);
        if (cooldown < 1) {
            result.error("Feature 'zone_denial' in profile '" + profile.id()
                    + "': cooldown_ticks must be >= 1 (got " + cooldown + ")");
        }

        var goalDuration = config.getInt("goal_duration_ticks", 80);
        if (goalDuration < 1) {
            result.error("Feature 'zone_denial' in profile '" + profile.id()
                    + "': goal_duration_ticks must be >= 1 (got " + goalDuration + ")");
        }

        var hazardLifetime = config.getInt("hazard_lifetime_ticks", 60);
        if (hazardLifetime < 1) {
            result.error("Feature 'zone_denial' in profile '" + profile.id()
                    + "': hazard_lifetime_ticks must be >= 1 (got " + hazardLifetime + ")");
        }

        var hazardRadius = config.getDouble("hazard_radius", 2.0);
        if (hazardRadius <= 0) {
            result.error("Feature 'zone_denial' in profile '" + profile.id()
                    + "': hazard_radius must be > 0 (got " + hazardRadius + ")");
        }

        var interval = config.getInt("place_interval_ticks", 20);
        if (interval < 1) {
            result.error("Feature 'zone_denial' in profile '" + profile.id()
                    + "': place_interval_ticks must be >= 1 (got " + interval + ")");
        }

        var maxHazards = config.getInt("max_hazards", 4);
        if (maxHazards < 1) {
            result.error("Feature 'zone_denial' in profile '" + profile.id()
                    + "': max_hazards must be >= 1 (got " + maxHazards + ")");
        }

        var minDist = config.getDouble("min_place_distance", 4.0);
        if (minDist <= 0) {
            result.error("Feature 'zone_denial' in profile '" + profile.id()
                    + "': min_place_distance must be > 0 (got " + minDist + ")");
        }

        var stateName = config.getString("state", "APPROACHING");
        try {
            MobState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            result.error("Feature 'zone_denial' in profile '" + profile.id()
                    + "': unknown state '" + stateName + "'");
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var abilityId      = config.getString("ability_id",          "zone_denial");
        var cooldown       = config.getInt("cooldown_ticks",         120);
        var goalDuration   = config.getInt("goal_duration_ticks",    80);
        var hazardLifetime = config.getInt("hazard_lifetime_ticks",  60);
        var hazardRadius   = config.getDouble("hazard_radius",       2.0);
        var hazardDmg      = (float) config.getDouble("hazard_damage_per_tick", 0.5);
        var interval       = config.getInt("place_interval_ticks",   20);
        var maxHazards     = config.getInt("max_hazards",            4);
        var minDist        = config.getDouble("min_place_distance",  4.0);
        var approachSpeed  = config.getDouble("approach_speed",      1.2);
        var state          = MobState.valueOf(config.getString("state", "APPROACHING"));
        var priority       = config.getInt("priority",               20);
        var hooks          = config.hooks();

        GoalEffects<MobEntity> effects = GoalEffects.<MobEntity>builder().build();
        if (hooks != null) effects = GoalEffects.compose(effects, hooks.toGoalEffects());

        var goal = new ZoneDenialBrainGoal<MobEntity>(
                abilityId, cooldown, goalDuration, hazardLifetime,
                hazardRadius, hazardDmg, interval, maxHazards, minDist, approachSpeed)
                .withEffects(effects);

        composer.addGoal(goal, Set.of(state), priority);
    }
}
