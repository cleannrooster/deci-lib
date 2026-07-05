package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.common.RangePunishBrainGoal;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.validation.ValidationResult;
import net.minecraft.entity.mob.MobEntity;

import java.util.Set;


public final class RangePunishFeature implements BehaviorFeature {

    @Override
    public String typeId() { return "range_punish"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var cooldown = config.getInt("cooldown_ticks", 100);
        if (cooldown < 1) {
            result.error("Feature 'range_punish' in profile '" + profile.id()
                    + "': cooldown_ticks must be >= 1 (got " + cooldown + ")");
        }

        var windup = config.getInt("windup_ticks", 20);
        if (windup < 1) {
            result.error("Feature 'range_punish' in profile '" + profile.id()
                    + "': windup_ticks must be >= 1 (got " + windup + ")");
        }

        var safeZone = config.getDouble("safe_zone_radius", 1.5);
        if (safeZone < 0) {
            result.error("Feature 'range_punish' in profile '" + profile.id()
                    + "': safe_zone_radius must be >= 0 (got " + safeZone + ")");
        }

        var waveWidth = config.getDouble("wave_width", 2.0);
        if (waveWidth <= 0) {
            result.error("Feature 'range_punish' in profile '" + profile.id()
                    + "': wave_width must be > 0 (got " + waveWidth + ")");
        }

        var waveSpeed = config.getDouble("wave_speed", 1.0);
        if (waveSpeed <= 0) {
            result.error("Feature 'range_punish' in profile '" + profile.id()
                    + "': wave_speed must be > 0 (got " + waveSpeed + ")");
        }

        var maxRange = config.getDouble("max_range", 16.0);
        if (maxRange <= safeZone) {
            result.error("Feature 'range_punish' in profile '" + profile.id()
                    + "': max_range must be > safe_zone_radius (got " + maxRange + ")");
        }

        var stateName = config.getString("state", "APPROACHING");
        try {
            MobState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            result.error("Feature 'range_punish' in profile '" + profile.id()
                    + "': unknown state '" + stateName + "'");
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var abilityId    = config.getString("ability_id",          "range_punish");
        var cooldown     = config.getInt("cooldown_ticks",         100);
        var interruptCd  = config.getInt("interrupt_cooldown_ticks", 30);
        var windup       = config.getInt("windup_ticks",           20);
        var safeZone     = config.getDouble("safe_zone_radius",    1.5);
        var waveWidth    = config.getDouble("wave_width",          2.0);
        var waveSpeed    = config.getDouble("wave_speed",          1.0);
        var maxRange     = config.getDouble("max_range",           16.0);
        var damage       = (float) config.getDouble("damage",      0.0);
        var coeff        = (float) config.getDouble("coeff",       0.8);
        var state        = MobState.valueOf(config.getString("state", "APPROACHING"));
        var priority     = config.getInt("priority",               35);
        var hooks        = config.hooks();

        var onStart     = ParticleStyle.fromString(config.getString("on_start_particles",     "none"));
        var onComplete  = ParticleStyle.fromString(config.getString("on_complete_particles",  "none"));
        var onInterrupt = ParticleStyle.fromString(config.getString("on_interrupt_particles", "none"));

        GoalEffects<MobEntity> effects = GoalEffects.<MobEntity>builder()
                .onStart(    (mob, world) -> onStart.spawn(mob, world, 0f, 0f))
                .onComplete( (mob, world) -> onComplete.spawn(mob, world, 0f, 0f))
                .onInterrupt((mob, world) -> onInterrupt.spawn(mob, world, 0f, 0f))
                .build();
        if (hooks != null) effects = GoalEffects.compose(effects, hooks.toGoalEffects());

        var goal = new RangePunishBrainGoal<MobEntity>(
                abilityId, cooldown, interruptCd,
                windup, safeZone, waveWidth, waveSpeed, maxRange, damage, coeff)
                .withEffects(effects);

        composer.addOffensiveGoal(goal, Set.of(state), priority);
    }
}
