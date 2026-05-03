package com.cleannrooster.decilib.builder.feature;

import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.statemachine.TransitionContext;
import com.cleannrooster.decilib.builder.MobStance;
import com.cleannrooster.decilib.builder.MobState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;


public final class BehaviorComposer {

    public record GoalEntry(
            MobBrainGoal<?>  goal,
            Set<MobStance>   stances,
            Set<MobState>    states,
            int              priority
    ) {}

    public record TransitionEntry(
            @Nullable MobState              from,
            Predicate<TransitionContext>    condition,
            MobState                        to,
            @Nullable MobStance             stanceFilter,
            int                             priority
    ) {}

    /** A {@link com.cleannrooster.decilib.ai.statemachine.StanceTransition}-shaped entry. */
    public record StanceTransitionEntry(
            @Nullable MobStance             from,
            Predicate<TransitionContext>    condition,
            MobStance                       to,
            int                             priority
    ) {}

    private final List<GoalEntry>            goals            = new ArrayList<>();
    private final List<TransitionEntry>      transitions      = new ArrayList<>();
    private final List<StanceTransitionEntry> stanceTransitions = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    public void addGoal(MobBrainGoal<?> goal, Set<MobStance> stances, Set<MobState> states, int priority) {
        goals.add(new GoalEntry(goal, Set.copyOf(stances), Set.copyOf(states), priority));
    }

    /** Convenience overload — goal valid in any stance. */
    public void addGoal(MobBrainGoal<?> goal, Set<MobState> states, int priority) {
        addGoal(goal, Set.of(), states, priority);
    }

    public void addTransition(@Nullable MobState from, Predicate<TransitionContext> condition,
                               MobState to, @Nullable MobStance stanceFilter, int priority) {
        transitions.add(new TransitionEntry(from, condition, to, stanceFilter, priority));
    }

    public void addStanceTransition(@Nullable MobStance from, Predicate<TransitionContext> condition,
                                     MobStance to, int priority) {
        stanceTransitions.add(new StanceTransitionEntry(from, condition, to, priority));
    }


    public List<GoalEntry>             goals()             { return Collections.unmodifiableList(goals); }
    public List<TransitionEntry>       transitions()       { return Collections.unmodifiableList(transitions); }
    public List<StanceTransitionEntry> stanceTransitions() { return Collections.unmodifiableList(stanceTransitions); }
}
