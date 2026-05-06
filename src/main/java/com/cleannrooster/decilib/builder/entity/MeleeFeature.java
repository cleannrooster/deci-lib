package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.common.MeleeAttackBrainGoal;
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


public final class MeleeFeature implements BehaviorFeature {

    @Override
    public String typeId() { return "melee"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var cooldown = config.getInt("cooldown_ticks", 20);
        if (cooldown < 1) {
            result.error("Feature 'melee' in profile '" + profile.id()
                    + "': cooldown_ticks must be >= 1 (got " + cooldown + ")");
        }
        var stateName = config.getString("state", "ATTACKING_MELEE");
        try {
            MobState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            result.error("Feature 'melee' in profile '" + profile.id()
                    + "': unknown state '" + stateName + "'");
        }
        if ("ambusher".equals(profile.archetype()) && "ATTACKING_MELEE".equals(stateName)) {
            result.warn("Feature 'melee' in profile '" + profile.id()
                    + "': ambusher archetype never enters ATTACKING_MELEE — "
                    + "add \"state\": \"ACTIVE\" or the attack will never fire");
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var abilityId = config.getString("ability_id",    "melee");
        var cooldown  = config.getInt("cooldown_ticks",   20);
        var state     = MobState.valueOf(config.getString("state", "ATTACKING_MELEE"));
        var priority  = config.getInt("priority",         10);

        var hooks = config.hooks();

        GoalEffects<MobEntity> effects = hooks != null ? hooks.toGoalEffects() : null;

        var goal = new MeleeAttackBrainGoal<MobEntity>(abilityId, cooldown);
        if (effects != null) goal.withEffects(effects);

        composer.addOffensiveTransition(null,
                ctx -> ctx.stimulus().targetInMeleeRange()
                        && ctx.cooldowns().isReady(abilityId),
                state, null, priority);

        composer.addGoal(goal, Set.of(state), priority);
    }
}
