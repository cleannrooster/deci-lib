package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.common.RadialAttackBrainGoal;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.feature.FeatureHooks;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.validation.ValidationResult;
import net.minecraft.entity.mob.MobEntity;

import java.util.Set;


public final class ShockwaveFeature implements BehaviorFeature {

    private static final String DEFAULT_STATE = "ATTACKING_MELEE";

    @Override
    public String typeId() { return "shockwave"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var range = config.getDouble("range", 4.0);
        if (range <= 0) {
            result.error("Feature 'shockwave' in profile '" + profile.id()
                    + "': range must be > 0 (got " + range + ")");
        }
        var stateName = config.getString("state", DEFAULT_STATE);
        try {
            MobState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            result.error("Feature 'shockwave' in profile '" + profile.id()
                    + "': unknown state '" + stateName + "'");
        }
        if ("ambusher".equals(profile.archetype()) && DEFAULT_STATE.equals(stateName)) {
            result.warn("Feature 'shockwave' in profile '" + profile.id()
                    + "': ambusher archetype never enters ATTACKING_MELEE — "
                    + "add \"state\": \"ACTIVE\" to this feature or the shockwave will never fire");
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var abilityId = config.getString("ability_id",    "shockwave");
        var cooldown  = config.getInt("cooldown_ticks",   80);
        var minCd     = config.getInt("min_cooldown",     20);
        var range     = config.getDouble("range",         4.0);
        var coeff     = (float) config.getDouble("coeff", 1.0);
        var state     = MobState.valueOf(config.getString("state", DEFAULT_STATE));
        var priority  = config.getInt("priority",         20);

        var finalRange = (float) range;
        var onStart    = ParticleStyle.fromString(config.getString("on_start_particles",    "none"));
        var onComplete = ParticleStyle.fromString(config.getString("on_complete_particles", "smoke_ring"));
        var hooks = config.hooks();

        GoalEffects<MobEntity> effects = GoalEffects.<MobEntity>builder()
                .onStart(   (mob, world) -> onStart.spawn(mob, world, finalRange, 0f))
                .onComplete((mob, world) -> onComplete.spawn(mob, world, finalRange, 0f))
                .build();
        if (hooks != null) effects = GoalEffects.compose(effects, hooks.toGoalEffects());

        var goal =
                new RadialAttackBrainGoal<MobEntity>(abilityId, cooldown, minCd, range, 0f, coeff)
                        .withEffects(effects);
        composer.addOffensiveGoal(goal, Set.of(state), priority);
    }
}
