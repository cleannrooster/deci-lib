package com.cleannrooster.decilib.ai.profile;

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
