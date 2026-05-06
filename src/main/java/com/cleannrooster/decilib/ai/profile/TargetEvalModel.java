package com.cleannrooster.decilib.ai.profile;

import com.cleannrooster.decilib.ai.statemachine.TransitionContext;

import java.util.function.Predicate;

public enum TargetEvalModel {

    /** Full aggression toward a single target; ignores all others. */
    SOLO_PREDATOR {
        @Override
        public Predicate<TransitionContext> offensiveGate() {
            return ctx -> true;
        }
    },

    /**
     * Deprioritizes its primary target when allies are under attack.
     * Phase 1 stub — real gate requires ally damage event tracking.
     */
    PACK_HUNTER {
        @Override
        public Predicate<TransitionContext> offensiveGate() {
            // Phase 1 stub: always passes. Real implementation needs ctx.stimulus().allyUnderAttack().
            return ctx -> true;
        }
    },

    /**
     * Suppresses offensive actions when the target has companions nearby.
     * Becomes significantly more dangerous when alone with its target.
     */
    COWARD {
        @Override
        public Predicate<TransitionContext> offensiveGate() {
            return ctx -> ctx.stimulus().nearbyThreatCount() <= 1;
        }
    };

    public abstract Predicate<TransitionContext> offensiveGate();
}
