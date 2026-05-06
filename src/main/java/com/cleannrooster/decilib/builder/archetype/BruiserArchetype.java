package com.cleannrooster.decilib.builder.archetype;

import com.cleannrooster.decilib.ai.goal.common.ApproachTargetBrainGoal;
import com.cleannrooster.decilib.ai.profile.AdaptationModel;
import com.cleannrooster.decilib.ai.profile.AggressionModel;
import com.cleannrooster.decilib.ai.profile.AiProfile;
import com.cleannrooster.decilib.ai.profile.SpatialModel;
import com.cleannrooster.decilib.ai.profile.TargetEvalModel;
import com.cleannrooster.decilib.builder.MobStance;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.tuning.TuningResolver;

import java.util.Set;

/**
 * Archetype for heavy melee mobs that charge their target and attack at close range.
 *
 * <p>State topology: {@code IDLE → APPROACHING → ATTACKING_MELEE}.
 * High health, high damage, low movement speed.
 */
public final class BruiserArchetype implements Archetype {

    @Override
    public String id() { return "bruiser"; }

    @Override
    public Set<String> requiredFeatures() { return Set.of(); }

    @Override
    public Set<String> incompatibleFeatures() { return Set.of(); }

    @Override
    public Set<MobState> supportedStates() {
        return Set.of(MobState.IDLE, MobState.APPROACHING, MobState.ATTACKING_MELEE);
    }

    @Override
    public MobStance initialStance() { return MobStance.AGGRESSIVE; }

    @Override
    public MobState initialState()   { return MobState.IDLE; }

    @Override
    public BaseStats baseStats(TuningProfile tuning) {
        return new BaseStats(
                TuningResolver.maxHealth(tuning.health()),
                TuningResolver.attackDamage(tuning.damage()),
                TuningResolver.movementSpeed(tuning.speed()),
                TuningResolver.followRange(tuning.detection()),
                TuningResolver.knockbackResistance(tuning.health()), // sturdier = more KB resist
                2.5
        );
    }

    @Override
    public AiProfile defaultAiProfile() {
        return new AiProfile(AggressionModel.RELENTLESS, SpatialModel.ANYWHERE,
                AdaptationModel.STATIC, TargetEvalModel.SOLO_PREDATOR);
    }

    @Override
    public void apply(BehaviorComposer composer, TuningProfile tuning) {
        var speed = TuningResolver.movementSpeed(tuning.speed()) / 0.28; // normalise to 1.0 base

        // Transitions — null from = any state
        composer.addTransition(null,
                ctx -> !ctx.stimulus().hasTarget(),
                MobState.IDLE, null, 0);

        composer.addTransition(null,
                ctx -> ctx.stimulus().hasTarget() && !ctx.stimulus().targetInMeleeRange(),
                MobState.APPROACHING, null, 5);

        // Goals
        composer.addGoal(new ApproachTargetBrainGoal<>(speed),
                Set.of(MobState.APPROACHING), 5);
    }
}
