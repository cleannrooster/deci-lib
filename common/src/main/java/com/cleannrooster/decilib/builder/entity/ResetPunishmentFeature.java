package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.common.ResetPunishmentBrainGoal;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.validation.ValidationResult;
import net.minecraft.entity.mob.MobEntity;

import java.util.Set;


public final class ResetPunishmentFeature implements BehaviorFeature {

    @Override
    public String typeId() { return "reset_punishment"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var cooldown = config.getInt("cooldown_ticks", 200);
        if (cooldown < 1) {
            result.error("Feature 'reset_punishment' in profile '" + profile.id()
                    + "': cooldown_ticks must be >= 1 (got " + cooldown + ")");
        }

        var rangeThreshold = config.getDouble("range_threshold", 10.0);
        if (rangeThreshold <= 0) {
            result.error("Feature 'reset_punishment' in profile '" + profile.id()
                    + "': range_threshold must be > 0 (got " + rangeThreshold + ")");
        }

        var timeAtRange = config.getInt("time_at_range_ticks", 100);
        if (timeAtRange < 1) {
            result.error("Feature 'reset_punishment' in profile '" + profile.id()
                    + "': time_at_range_ticks must be >= 1 (got " + timeAtRange + ")");
        }

        var windup = config.getInt("windup_ticks", 40);
        if (windup < 1) {
            result.error("Feature 'reset_punishment' in profile '" + profile.id()
                    + "': windup_ticks must be >= 1 (got " + windup + ")");
        }

        var healAmount   = config.getDouble("heal_amount",   0.0);
        var healFraction = config.getDouble("heal_fraction", 0.0);
        if (healAmount <= 0 && healFraction <= 0) {
            result.warn("Feature 'reset_punishment' in profile '" + profile.id()
                    + "': neither heal_amount nor heal_fraction is set — mob will not heal");
        }
        if (healFraction > 1) {
            result.error("Feature 'reset_punishment' in profile '" + profile.id()
                    + "': heal_fraction must be <= 1 (got " + healFraction + ")");
        }

        var stateName = config.getString("state", "APPROACHING");
        try {
            MobState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            result.error("Feature 'reset_punishment' in profile '" + profile.id()
                    + "': unknown state '" + stateName + "'");
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var abilityId       = config.getString("ability_id",             "reset_punishment");
        var cooldown        = config.getInt("cooldown_ticks",            200);
        var interruptCd     = config.getInt("interrupt_cooldown_ticks",  40);
        var rangeThreshold  = config.getDouble("range_threshold",        10.0);
        var timeAtRange     = config.getInt("time_at_range_ticks",       100);
        var windup          = config.getInt("windup_ticks",              40);
        var interruptRange  = config.getDouble("interrupt_range",        rangeThreshold * 0.6);
        var healAmount      = (float) config.getDouble("heal_amount",    0.0);
        var healFraction    = (float) config.getDouble("heal_fraction",  0.0);
        var approachSpeed   = config.getDouble("approach_speed",         1.2);
        var state           = MobState.valueOf(config.getString("state", "APPROACHING"));
        var priority        = config.getInt("priority",                  15);
        var hooks           = config.hooks();

        var onComplete  = ParticleStyle.fromString(config.getString("on_complete_particles",  "none"));
        var onInterrupt = ParticleStyle.fromString(config.getString("on_interrupt_particles", "none"));

        GoalEffects<MobEntity> effects = GoalEffects.<MobEntity>builder()
                .onComplete( (mob, world) -> onComplete.spawn(mob, world, 0f, 0f))
                .onInterrupt((mob, world) -> onInterrupt.spawn(mob, world, 0f, 0f))
                .build();
        if (hooks != null) effects = GoalEffects.compose(effects, hooks.toGoalEffects());

        var goal = new ResetPunishmentBrainGoal<MobEntity>(
                abilityId, cooldown, interruptCd,
                rangeThreshold, timeAtRange, windup,
                interruptRange, healAmount, healFraction, approachSpeed)
                .withEffects(effects);

        composer.addGoal(goal, Set.of(state), priority);
    }
}
