package com.cleannrooster.decilib.builder.archetype;

import com.cleannrooster.decilib.ai.goal.common.ApproachTargetBrainGoal;
import com.cleannrooster.decilib.ai.goal.common.FleeEntityBrainGoal;
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


public final class SkirmisherArchetype implements Archetype {

    private static final float FLEE_HEALTH = 0.30f;

    @Override
    public String id() { return "skirmisher"; }

    @Override
    public Set<String> requiredFeatures() { return Set.of(); }

    @Override
    public Set<String> incompatibleFeatures() { return Set.of(); }

    @Override
    public Set<MobState> supportedStates() {
        return Set.of(
                MobState.IDLE, MobState.APPROACHING, MobState.ATTACKING_MELEE,
                MobState.FLEEING, MobState.RETREATING,
                MobState.CHARGING,           // used by ChargeFeature
                MobState.ACTIVE,             // used by AmbushAttackFeature
                MobState.HIDDEN, MobState.REHIDING
        );
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
                0.0,
                2.0
        );
    }

    @Override
    public AiProfile defaultAiProfile() {
        return new AiProfile(AggressionModel.CALCULATING, SpatialModel.ANYWHERE,
                AdaptationModel.STATIC, TargetEvalModel.SOLO_PREDATOR);
    }

    @Override
    public void apply(BehaviorComposer composer, TuningProfile tuning) {
        var speed     = TuningResolver.movementSpeed(tuning.speed()) / 0.28;
        var fleeSpeed = speed * 1.3;
        var fleeRange = TuningResolver.followRange(tuning.detection());
        var profile   = composer.aiProfile();

        // Low-health flee (always active, regardless of aggression axis)
        composer.addTransition(MobState.IDLE,
                ctx -> ctx.stimulus().hasTarget() && ctx.stimulus().selfHealthPct() < FLEE_HEALTH,
                MobState.FLEEING, null, 15);
        composer.addTransition(MobState.APPROACHING,
                ctx -> ctx.stimulus().hasTarget() && ctx.stimulus().selfHealthPct() < FLEE_HEALTH,
                MobState.FLEEING, null, 15);
        composer.addTransition(MobState.ATTACKING_MELEE,
                ctx -> ctx.stimulus().hasTarget() && ctx.stimulus().selfHealthPct() < FLEE_HEALTH,
                MobState.FLEEING, null, 15);

        composer.addTransition(MobState.FLEEING,
                ctx -> !ctx.stimulus().hasTarget()
                    || ctx.stimulus().targetDistance() > fleeRange,
                MobState.IDLE, null, 20);

        // CALCULATING: active retreat when at a health disadvantage (>20% below target).
        // Priority 8 — below low-health flee (15) but above normal approach (5).
        if (profile.aggression() == AggressionModel.CALCULATING) {
            composer.addTransition(MobState.APPROACHING,
                    ctx -> ctx.stimulus().hasTarget()
                            && ctx.stimulus().selfHealthPct() < ctx.stimulus().targetHealthPct() - 0.2f,
                    MobState.RETREATING, null, 8);
            composer.addTransition(MobState.ATTACKING_MELEE,
                    ctx -> ctx.stimulus().hasTarget()
                            && ctx.stimulus().selfHealthPct() < ctx.stimulus().targetHealthPct() - 0.2f,
                    MobState.RETREATING, null, 8);

            composer.addTransition(MobState.RETREATING,
                    ctx -> !ctx.stimulus().hasTarget()
                            || ctx.stimulus().selfHealthPct() >= ctx.stimulus().targetHealthPct() - 0.1f,
                    MobState.APPROACHING, null, 15);

            composer.addGoal(new FleeEntityBrainGoal<>(fleeRange, fleeSpeed),
                    Set.of(MobState.RETREATING), 8);
        }

        composer.addTransition(null,
                ctx -> !ctx.stimulus().hasTarget(),
                MobState.IDLE, null, 0);

        composer.addTransition(null,
                ctx -> ctx.stimulus().hasTarget() && !ctx.stimulus().targetInMeleeRange(),
                MobState.APPROACHING, null, 5);

        // Goals — hold at standoff when conditions aren't favourable
        composer.addGoal(
                new ApproachTargetBrainGoal<>(speed)
                        .withStandoff(8.0, profile.combinedStimulusGate()),
                Set.of(MobState.APPROACHING), 5);

        composer.addGoal(new FleeEntityBrainGoal<>(fleeRange, fleeSpeed),
                Set.of(MobState.FLEEING), 15);
    }
}
