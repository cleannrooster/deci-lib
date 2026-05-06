package com.cleannrooster.decilib.ai.profile;

import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import com.cleannrooster.decilib.ai.statemachine.TransitionContext;

import java.util.function.Predicate;

public enum SpatialModel {

    /** No spatial requirement — attacks from any position. */
    ANYWHERE {
        @Override
        public Predicate<TransitionContext> offensiveGate() { return ctx -> true; }
        @Override
        public Predicate<AIStimulus> stimulusGate()         { return s -> true; }
    },

    /**
     * Anchors to its spawn position. Only commits to offensive actions when
     * within its follow range of that anchor.
     */
    TERRITORIAL {
        @Override
        public Predicate<TransitionContext> offensiveGate() {
            return ctx -> ctx.stimulus().isNearAnchor();
        }
        @Override
        public Predicate<AIStimulus> stimulusGate() {
            return AIStimulus::isNearAnchor;
        }
    },

    /**
     * Prefers to strike when the target is near an obstacle or in a corner.
     * Phase 1 stub — real gate requires raycasting ({@code targetNearObstacle()}).
     */
    PREDATORY {
        @Override
        public Predicate<TransitionContext> offensiveGate() {
            // Phase 1 stub: always passes. Real implementation needs ctx.stimulus().targetNearObstacle().
            return ctx -> true;
        }
        @Override
        public Predicate<AIStimulus> stimulusGate() {
            // Phase 1 stub: always passes. Real implementation needs s.targetNearObstacle().
            return s -> true;
        }
    },

    /**
     * Scales aggressiveness with allied presence. Only commits offensively
     * when at least 2 allied mobs are nearby.
     */
    SWARMER {
        @Override
        public Predicate<TransitionContext> offensiveGate() {
            return ctx -> ctx.stimulus().nearbyAllyCount() >= 2;
        }
        @Override
        public Predicate<AIStimulus> stimulusGate() {
            return s -> s.nearbyAllyCount() >= 2;
        }
    };

    public abstract Predicate<TransitionContext> offensiveGate();
    public abstract Predicate<AIStimulus> stimulusGate();
}
