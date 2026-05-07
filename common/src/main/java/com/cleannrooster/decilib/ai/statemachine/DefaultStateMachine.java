package com.cleannrooster.decilib.ai.statemachine;

import net.minecraft.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class DefaultStateMachine<S extends Enum<S>, C extends Enum<C>>
        implements MobStateMachine<S, C> {

    private S currentStance;
    private C currentCombatState;

    private final List<StanceTransition<S>> stanceTransitions = new ArrayList<>();
    private final List<StateTransition<S, C>> combatTransitions = new ArrayList<>();

    private final Set<Object> lockedStances = new HashSet<>();
    private final Set<Object> lockedCombatStates = new HashSet<>();

    private StanceLifecycleHandler<S> stanceHandler;
    private CombatStateLifecycleHandler<C> combatStateHandler;
    private LivingEntity boundEntity;

    public DefaultStateMachine(S initialStance, C initialCombatState) {
        this.currentStance = initialStance;
        this.currentCombatState = initialCombatState;
    }

    @Override
    public void bind(LivingEntity entity) {
        this.boundEntity = entity;
    }

    // -------------------------------------------------------------------------
    // Evaluation
    // -------------------------------------------------------------------------

    @Override
    public void evaluateStance(TransitionContext ctx) {
        StanceTransition<S> best = null;

        for (StanceTransition<S> t : stanceTransitions) {
            if (lockedStances.contains(t.toStance())) continue;
            if (t.fromStance() != null && t.fromStance() != currentStance) continue;
            if (!t.condition().test(ctx)) continue;
            if (best == null || t.priority() > best.priority()) {
                best = t;
            }
        }

        if (best != null && best.toStance() != currentStance) {
            var previous = currentStance;
            if (stanceHandler != null) stanceHandler.onStanceExit(previous, boundEntity);
            currentStance = best.toStance();
            if (stanceHandler != null) stanceHandler.onStanceEnter(currentStance, boundEntity);
        }
    }

    @Override
    public void evaluateCombatState(TransitionContext ctx) {
        StateTransition<S, C> best = null;

        for (StateTransition<S, C> t : combatTransitions) {
            if (lockedCombatStates.contains(t.toState())) continue;
            if (t.fromState() != null && t.fromState() != currentCombatState) continue;
            if (t.requiredStance() != null && t.requiredStance() != currentStance) continue;
            if (!t.condition().test(ctx)) continue;
            if (best == null || t.priority() > best.priority()) {
                best = t;
            }
        }

        if (best != null && best.toState() != currentCombatState) {
            var previous = currentCombatState;
            if (combatStateHandler != null) combatStateHandler.onCombatStateExit(previous, boundEntity);
            currentCombatState = best.toState();
            if (combatStateHandler != null) combatStateHandler.onCombatStateEnter(currentCombatState, boundEntity);
        }
    }

    // -------------------------------------------------------------------------
    // Current state
    // -------------------------------------------------------------------------

    @Override public S getCurrentStance()       { return currentStance; }
    @Override public C getCurrentCombatState()  { return currentCombatState; }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    @Override
    public void registerStanceTransition(StanceTransition<S> transition) {
        stanceTransitions.add(transition);
    }

    @Override
    public void registerTransition(StateTransition<S, C> transition) {
        combatTransitions.add(transition);
    }

    // -------------------------------------------------------------------------
    // Lockouts
    // -------------------------------------------------------------------------

    @Override
    public void applyStanceLockout(Collection<?> stances) {
        lockedStances.addAll(stances);
    }

    @Override
    public void applyCombatStateLockout(Collection<?> states) {
        lockedCombatStates.addAll(states);
    }

    // -------------------------------------------------------------------------
    // Callback registration
    // -------------------------------------------------------------------------

    @Override public void setStanceHandler(StanceLifecycleHandler<S> handler)               { this.stanceHandler = handler; }
    @Override public void setCombatStateHandler(CombatStateLifecycleHandler<C> handler)     { this.combatStateHandler = handler; }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    @Override
    public Collection<C> referencedCombatStates() {
        var states = new HashSet<C>();
        for (StateTransition<S, C> t : combatTransitions) {
            states.add(t.toState());
        }
        return states;
    }

    @Override
    public void validateTransitions(Set<Object> lockedStates, Set<Object> lockedStances, Consumer<String> warn) {
        for (StateTransition<S, C> t : combatTransitions) {
            if (lockedStates.contains(t.toState())) {
                warn.accept("StateTransition to " + t.toState()
                        + " is permanently locked out by a PhaseTransition");
            }
        }
        for (StanceTransition<S> t : stanceTransitions) {
            if (lockedStances.contains(t.toStance())) {
                warn.accept("StanceTransition to " + t.toStance()
                        + " is permanently locked out by a PhaseTransition");
            }
        }
    }
}
