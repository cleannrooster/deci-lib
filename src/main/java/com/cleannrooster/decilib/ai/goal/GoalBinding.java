package com.cleannrooster.decilib.ai.goal;

import net.minecraft.entity.LivingEntity;

import java.util.Set;

public record GoalBinding<E extends LivingEntity, S extends Enum<S>, C extends Enum<C>>(
        MobBrainGoal<E> goal,
        Set<S> validStances,
        Set<C> validCombatStates,
        int priority
) {

    public boolean isValidIn(S stance, C combatState) {
        var stanceOk = validStances.isEmpty() || validStances.contains(stance);
        var stateOk  = validCombatStates.isEmpty() || validCombatStates.contains(combatState);
        return stanceOk && stateOk;
    }
}
