package com.cleannrooster.decilib.ai.profile;

import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import com.cleannrooster.decilib.ai.statemachine.TransitionContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Fully-resolved AI personality for a mob. Describes how the mob evaluates the
 * world across four independent axes. Features AND their base transition
 * condition with {@link #combinedGate()} to express the archetype's reasoning
 * philosophy, not just its state topology.
 *
 * <p>Resolved during {@code MobBuilder.build()}: archetype default first,
 * then per-axis overrides from the mob JSON {@code ai_profile} block.
 */
public record AiProfile(
        AggressionModel aggression,
        SpatialModel    spatial,
        AdaptationModel adaptation,
        TargetEvalModel targetEval
) {

    /**
     * Returns a predicate that must pass before any offensive feature fires.
     * Combines all four axes with logical AND.
     */
    public Predicate<TransitionContext> combinedGate() {
        return aggression.offensiveGate()
                .and(spatial.offensiveGate())
                .and(adaptation.offensiveGate())
                .and(targetEval.offensiveGate());
    }

    /**
     * Stimulus-only version of {@link #combinedGate()} for goals that receive
     * only {@link AIStimulus} (e.g. {@code ApproachTargetBrainGoal.canStart}).
     */
    public Predicate<AIStimulus> combinedStimulusGate() {
        return aggression.stimulusGate()
                .and(spatial.stimulusGate())
                .and(adaptation.stimulusGate())
                .and(targetEval.stimulusGate());
    }

    // ── Last-stand thresholds ────────────────────────────────────────────────

    /** Health fraction below which near-death trigger fires. */
    private static final float LAST_STAND_HEALTH    = 0.15f;
    /** Recent damage (as fraction of max health) that triggers a burst-panic last stand. */
    private static final float BURST_DAMAGE_PCT     = 0.20f;
    /** Cumulative fight damage fraction above which the mob is considered "ground down". */
    private static final float CRUSHED_PROGRESS     = 0.75f;
    /** If the target is still this healthy when the mob is ground down, hope is lost. */
    private static final float CRUSHED_TARGET_FLOOR = 0.65f;
    /** Threat count at or above which the mob is hopelessly surrounded. */
    private static final int   HOPELESS_THREATS     = 3;

    /**
     * Last-stand override gate. When this passes, offensive transitions fire
     * regardless of {@link #combinedGate()} (logical OR), and the mob enters
     * {@code MobState.LAST_STAND} for a desperate final push.
     *
     * <p>Trigger conditions (any one activates):
     * <ul>
     *   <li>Near death: {@code selfHealthPct < 0.15}</li>
     *   <li>Burst hit: recent damage spike ≥ 20% of max health</li>
     * </ul>
     *
     * <p>Blocker conditions (any one suppresses, evaluated after triggers):
     * <ul>
     *   <li>OPPORTUNIST aggression — only fights on favourable odds</li>
     *   <li>COWARD target-eval with any nearby threat</li>
     *   <li>Crushed: {@code fightProgressPct > 0.75} AND target still at {@code > 0.65} health</li>
     *   <li>Hopelessly surrounded: {@code nearbyThreatCount >= 3}</li>
     * </ul>
     */
    public Predicate<TransitionContext> lastStandGate() {
        return ctx -> {
            var s = ctx.stimulus();
            if (!s.hasTarget()) return false;

            // Triggers — at least one must be true
            boolean nearDeath     = s.selfHealthPct() < LAST_STAND_HEALTH;
            boolean burstTriggered = s.recentDamagePct() >= BURST_DAMAGE_PCT;
            if (!nearDeath && !burstTriggered) return false;

            // Blockers — any one vetoes last stand
            if (aggression == AggressionModel.OPPORTUNIST) return false;
            if (targetEval == TargetEvalModel.COWARD && s.nearbyThreatCount() > 0) return false;
            if (s.fightProgressPct() > CRUSHED_PROGRESS
                    && s.targetHealthPct() > CRUSHED_TARGET_FLOOR) return false;
            if (s.nearbyThreatCount() >= HOPELESS_THREATS) return false;

            return true;
        };
    }

    /**
     * Returns a new {@code AiProfile} where each non-null argument overrides
     * the corresponding axis from this profile. Used by {@code MobBuilder} to
     * apply partial JSON overrides on top of the archetype default.
     */
    public AiProfile merge(
            @Nullable AggressionModel aggr,
            @Nullable SpatialModel    spat,
            @Nullable AdaptationModel adapt,
            @Nullable TargetEvalModel target) {
        return new AiProfile(
                aggr   != null ? aggr   : this.aggression,
                spat   != null ? spat   : this.spatial,
                adapt  != null ? adapt  : this.adaptation,
                target != null ? target : this.targetEval
        );
    }
}
