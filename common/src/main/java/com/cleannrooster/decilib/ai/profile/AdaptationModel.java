package com.cleannrooster.decilib.ai.profile;

import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import com.cleannrooster.decilib.ai.statemachine.TransitionContext;

import java.util.function.Predicate;

public enum AdaptationModel {

    /** Behavior is identical throughout the fight. */
    STATIC {
        @Override
        public Predicate<TransitionContext> offensiveGate() { return ctx -> true; }
        @Override
        public Predicate<AIStimulus> stimulusGate()         { return s -> true; }
    },

    /**
     * Offensive features are locked until the mob has taken significant damage
     * (≥30% of max health). Models a mob that escalates in response to being hurt.
     */
    ESCALATING {
        @Override
        public Predicate<TransitionContext> offensiveGate() {
            return ctx -> ctx.stimulus().fightProgressPct() >= 0.30f;
        }
        @Override
        public Predicate<AIStimulus> stimulusGate() {
            return s -> s.fightProgressPct() >= 0.30f;
        }
    },

    /**
     * Biases toward features that recently landed and suppresses features that
     * were recently avoided.
     * Phase 1 stub — real gate requires per-ability hit tracking.
     */
    LEARNING {
        @Override
        public Predicate<TransitionContext> offensiveGate() {
            // Phase 1 stub: always passes. Real implementation needs per-ability hit rate data.
            return ctx -> true;
        }
        @Override
        public Predicate<AIStimulus> stimulusGate() {
            // Phase 1 stub: always passes. Real implementation needs per-ability hit rate data.
            return s -> true;
        }
    };

    public abstract Predicate<TransitionContext> offensiveGate();
    public abstract Predicate<AIStimulus> stimulusGate();
}
