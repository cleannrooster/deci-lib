package com.cleannrooster.decilib.ai.statemachine;

import net.minecraft.entity.LivingEntity;


public interface CombatStateLifecycleHandler<C extends Enum<C>> {

    void onCombatStateEnter(C newState, LivingEntity entity);

    void onCombatStateExit(C oldState, LivingEntity entity);
}
