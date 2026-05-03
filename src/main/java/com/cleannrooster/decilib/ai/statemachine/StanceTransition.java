package com.cleannrooster.decilib.ai.statemachine;

import java.util.function.Predicate;


public record StanceTransition<S extends Enum<S>>(
        S fromStance,
        Predicate<TransitionContext> condition,
        S toStance,
        int priority
) {}
