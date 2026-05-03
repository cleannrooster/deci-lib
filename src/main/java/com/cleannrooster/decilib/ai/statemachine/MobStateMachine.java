package com.cleannrooster.decilib.ai.statemachine;

import net.minecraft.entity.LivingEntity;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
public interface MobStateMachine<S extends Enum<S>, C extends Enum<C>> {

    void bind(LivingEntity entity);

    // -------------------------------------------------------------------------
    // Evaluation
    // -------------------------------------------------------------------------

    void evaluateStance(TransitionContext ctx);

    void evaluateCombatState(TransitionContext ctx);


    S getCurrentStance();

    C getCurrentCombatState();


    void registerStanceTransition(StanceTransition<S> transition);

    void registerTransition(StateTransition<S, C> transition);

    // -------------------------------------------------------------------------
    // Lockouts — called by the brain when a PhaseTransition fires
    // -------------------------------------------------------------------------


    void applyStanceLockout(Collection<?> stances);


    void applyCombatStateLockout(Collection<?> states);

    // -------------------------------------------------------------------------
    // Callback registration
    // -------------------------------------------------------------------------

    void setStanceHandler(StanceLifecycleHandler<S> handler);

    void setCombatStateHandler(CombatStateLifecycleHandler<C> handler);

    // -------------------------------------------------------------------------
    // Validation — called once during brain initialize()
    // -------------------------------------------------------------------------


    Collection<C> referencedCombatStates();


    void validateTransitions(Set<Object> lockedStates, Set<Object> lockedStances, Consumer<String> warn);
}
