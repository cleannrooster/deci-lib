package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.common.ArcAttackBrainGoal;
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


public final class SweepFeature implements BehaviorFeature {

    private static final String DEFAULT_STATE = "ATTACKING_MELEE";

    @Override
    public String typeId() { return "sweep"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var halfAngle = config.getDouble("half_angle_deg", 45.0);
        if (halfAngle <= 0 || halfAngle >= 180) {
            result.error("Feature 'sweep' in profile '" + profile.id()
                    + "': half_angle_deg must be in (0, 180) (got " + halfAngle + ")");
        }
        var range = config.getDouble("range", 3.5);
        if (range <= 0) {
            result.error("Feature 'sweep' in profile '" + profile.id()
                    + "': range must be > 0 (got " + range + ")");
        }
        var stateName = config.getString("state", DEFAULT_STATE);
        try {
            MobState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            result.error("Feature 'sweep' in profile '" + profile.id()
                    + "': unknown state '" + stateName + "'");
        }
        if ("ambusher".equals(profile.archetype()) && DEFAULT_STATE.equals(stateName)) {
            result.warn("Feature 'sweep' in profile '" + profile.id()
                    + "': ambusher archetype never enters ATTACKING_MELEE — "
                    + "add \"state\": \"ACTIVE\" to this feature or the sweep will never fire");
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var abilityId = config.getString("ability_id",     "sweep");
        var cooldown  = config.getInt("cooldown_ticks",    40);
        var minCd     = config.getInt("min_cooldown",      10);
        var halfAngle = (float) config.getDouble("half_angle_deg", 45.0);
        var range     = config.getDouble("range",          3.5);
        var coeff     = (float) config.getDouble("coeff",  0.8);
        var state     = MobState.valueOf(config.getString("state", DEFAULT_STATE));
        var priority  = config.getInt("priority",          15);

        var finalRange = (float) range;
        var onComplete = ParticleStyle.fromString(config.getString("on_complete_particles", "sweep_arc"));
        var hooks = config.hooks();

        GoalEffects<MobEntity> effects = GoalEffects.<MobEntity>builder()
                .onComplete((mob, world) -> onComplete.spawn(mob, world, finalRange, halfAngle))
                .build();
        if (hooks != null) effects = GoalEffects.compose(effects, hooks.toGoalEffects());

        var goal =
                new ArcAttackBrainGoal<MobEntity>(abilityId, cooldown, minCd, halfAngle, range, 0f, coeff)
                        .withEffects(effects);
        composer.addGoal(goal, Set.of(state), priority);
    }
}
