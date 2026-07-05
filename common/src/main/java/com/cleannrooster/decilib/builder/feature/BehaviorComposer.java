package com.cleannrooster.decilib.builder.feature;

import com.cleannrooster.decilib.ai.goal.GatedBrainGoal;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.profile.AiProfile;
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

    private AiProfile aiProfile;

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

    /**
     * Registers an offensive goal — one that represents attacking or pursuing the
     * target. Automatically wraps the goal with {@link AiProfile#combinedStimulusGate()}
     * so that {@code canStart} is suppressed when the AI profile's conditions are not
     * met (e.g. SWARMER requires allies nearby, OPPORTUNIST requires target below 50% health).
     *
     * <p>Use this instead of {@link #addGoal} for any goal that represents an attack,
     * pursuit, or offensive action. Defensive and utility goals (idle, brace, reposition,
     * leave_hazard, ambush lifecycle) should continue to use {@link #addGoal}.
     */
    public void addOffensiveGoal(MobBrainGoal<?> goal, Set<MobState> states, int priority) {
        var gate = aiProfile.combinedStimulusGate();
        addGoal(new GatedBrainGoal<>(goal, gate), states, priority);
    }

    /**
     * Shorthand for offensive entry transitions. Automatically applies
     * {@link AiProfile#combinedGate()} ANDed with {@code baseCondition},
     * OR'd with {@link AiProfile#lastStandGate()} so desperate mobs can still
     * commit even when normal conditions are unfavorable.
     *
     * <p>Use this instead of {@link #addTransition} for any transition that
     * represents a mob choosing to attack.
     */
    public void addOffensiveTransition(@Nullable MobState from,
            Predicate<TransitionContext> baseCondition, MobState to,
            @Nullable MobStance stanceFilter, int priority) {
        var gate      = aiProfile.combinedGate();
        var lastStand = aiProfile.lastStandGate();
        addTransition(from,
                ctx -> (baseCondition.test(ctx) && gate.test(ctx)) || lastStand.test(ctx),
                to, stanceFilter, priority);
    }


    public List<GoalEntry>             goals()             { return Collections.unmodifiableList(goals); }
    public List<TransitionEntry>       transitions()       { return Collections.unmodifiableList(transitions); }
    public List<StanceTransitionEntry> stanceTransitions() { return Collections.unmodifiableList(stanceTransitions); }

    /** Called by {@code MobBuilder} before any archetype or feature applies its transitions. */
    public void setAiProfile(AiProfile profile) { this.aiProfile = profile; }

    /** Returns the resolved {@link AiProfile} for this mob. Available during {@code BehaviorFeature.apply()}. */
    public AiProfile aiProfile() { return aiProfile; }
}
