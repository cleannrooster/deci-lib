package com.cleannrooster.decilib.ai.statemachine;

import java.util.function.Predicate;

public record StateTransition<S extends Enum<S>, C extends Enum<C>>(
        C fromState,
        Predicate<TransitionContext> condition,
        C toState,
        S requiredStance,
        int priority
) {}
