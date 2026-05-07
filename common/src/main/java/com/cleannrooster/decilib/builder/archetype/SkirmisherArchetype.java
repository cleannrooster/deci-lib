package com.cleannrooster.decilib.builder.archetype;

import com.cleannrooster.decilib.ai.goal.common.ApproachTargetBrainGoal;
import com.cleannrooster.decilib.ai.goal.common.FleeEntityBrainGoal;
import com.cleannrooster.decilib.ai.goal.common.SeekCoverBrainGoal;
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

    private static final float  FLEE_HEALTH               = 0.30f;
    private static final int    CALCULATING_RETREAT_TICKS = 80;
    private static final double CALCULATING_RETREAT_DIST  = 10.0;
    private static final double COVER_SEARCH_RADIUS       = 14.0;
    private static final double LAST_STAND_RUSH_MULT      = 1.4;

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
                MobState.RETREATING, MobState.LAST_STAND,
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
        var profile   = composer.aiProfile();

        // Low-health retreat — interrupts ranged goals regardless of aggression axis
        composer.addTransition(MobState.APPROACHING,
                ctx -> ctx.stimulus().hasTarget() && ctx.stimulus().selfHealthPct() < FLEE_HEALTH,
                MobState.RETREATING, null, 15);
        composer.addTransition(MobState.ATTACKING_MELEE,
                ctx -> ctx.stimulus().hasTarget() && ctx.stimulus().selfHealthPct() < FLEE_HEALTH,
                MobState.RETREATING, null, 15);
        // Fallback retreat exit + goal — for aggression models without a profile-specific retreat block
        composer.addTransition(MobState.RETREATING,
                ctx -> !ctx.stimulus().hasTarget() || ctx.stimulus().selfHealthPct() >= FLEE_HEALTH,
                MobState.APPROACHING, null, 10);
        composer.addGoal(new FleeEntityBrainGoal<>(CALCULATING_RETREAT_DIST, fleeSpeed),
                Set.of(MobState.RETREATING), 5);

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

            // Exit retreat when health recovers, LOS breaks, or enough time without being hit.
            composer.addTransition(MobState.RETREATING,
                    ctx -> !ctx.stimulus().hasTarget()
                            || ctx.stimulus().selfHealthPct() >= ctx.stimulus().targetHealthPct() - 0.1f
                            || !ctx.stimulus().hasLineOfSight()
                            || ctx.stimulus().ticksSinceLastHit() > CALCULATING_RETREAT_TICKS,
                    MobState.APPROACHING, null, 15);

            // Fixed safety distance — just establish a gap, not flee to max range.
            composer.addGoal(new FleeEntityBrainGoal<>(CALCULATING_RETREAT_DIST, fleeSpeed),
                    Set.of(MobState.RETREATING), 8);
        }

        // OPPORTUNIST: retreat when at a health disadvantage; actively seek cover to break LOS;
        // exit retreat only when LOS is broken (no time-based fallback).
        if (profile.aggression() == AggressionModel.OPPORTUNIST) {
            composer.addTransition(MobState.APPROACHING,
                    ctx -> ctx.stimulus().hasTarget()
                            && ctx.stimulus().selfHealthPct() < ctx.stimulus().targetHealthPct() - 0.2f,
                    MobState.RETREATING, null, 8);
            composer.addTransition(MobState.ATTACKING_MELEE,
                    ctx -> ctx.stimulus().hasTarget()
                            && ctx.stimulus().selfHealthPct() < ctx.stimulus().targetHealthPct() - 0.2f,
                    MobState.RETREATING, null, 8);

            composer.addTransition(MobState.RETREATING,
                    ctx -> !ctx.stimulus().hasTarget() || !ctx.stimulus().hasLineOfSight(),
                    MobState.APPROACHING, null, 15);

            composer.addGoal(new SeekCoverBrainGoal<>(COVER_SEARCH_RADIUS, fleeSpeed),
                    Set.of(MobState.RETREATING), 8);
        }

        // LAST_STAND: desperate final push when the lastStandGate fires (near death or burst hit,
        // unless profile blockers suppress it). Priority 22 overrides retreat (8) and flee (15).
        var lastStand = profile.lastStandGate();
        composer.addTransition(null,
                lastStand::test,
                MobState.LAST_STAND, null, 22);
        composer.addTransition(MobState.LAST_STAND,
                ctx -> !ctx.stimulus().hasTarget(),
                MobState.IDLE, null, 0);
        // Edge case: mob healed out of last-stand territory
        composer.addTransition(MobState.LAST_STAND,
                ctx -> ctx.stimulus().hasTarget() && !lastStand.test(ctx),
                MobState.APPROACHING, null, 10);
        composer.addGoal(
                new ApproachTargetBrainGoal<>(speed * LAST_STAND_RUSH_MULT),
                Set.of(MobState.LAST_STAND), 22);

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

    }
}
