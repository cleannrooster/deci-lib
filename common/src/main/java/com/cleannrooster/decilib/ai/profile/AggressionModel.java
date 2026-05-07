package com.cleannrooster.decilib.ai.profile;

import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import com.cleannrooster.decilib.ai.statemachine.TransitionContext;

import java.util.function.Predicate;

public enum AggressionModel {

    /** Always pursues and commits. Never breaks off due to health state. */
    RELENTLESS {
        @Override
        public Predicate<TransitionContext> offensiveGate() { return ctx -> true; }
        @Override
        public Predicate<AIStimulus> stimulusGate()         { return s -> true; }
    },

    /** Only commits when not at a significant health disadvantage vs. the target. */
    CALCULATING {
        @Override
        public Predicate<TransitionContext> offensiveGate() {
            return ctx -> ctx.stimulus().selfHealthPct() >= ctx.stimulus().targetHealthPct() - 0.1f;
        }
        @Override
        public Predicate<AIStimulus> stimulusGate() {
            return s -> s.selfHealthPct() >= s.targetHealthPct() - 0.1f;
        }
    },

    /** Only strikes when the target is already weakened (below 50% health). */
    OPPORTUNIST {
        @Override
        public Predicate<TransitionContext> offensiveGate() {
            return ctx -> ctx.stimulus().targetHealthPct() < 0.5f;
        }
        @Override
        public Predicate<AIStimulus> stimulusGate() {
            return s -> s.targetHealthPct() < 0.5f;
        }
    };

    public abstract Predicate<TransitionContext> offensiveGate();
    public abstract Predicate<AIStimulus> stimulusGate();
}
