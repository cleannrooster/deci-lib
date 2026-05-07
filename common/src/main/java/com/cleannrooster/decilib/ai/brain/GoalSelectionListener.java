package com.cleannrooster.decilib.ai.brain;

import net.minecraft.entity.LivingEntity;

@FunctionalInterface
public interface GoalSelectionListener<T extends LivingEntity, S extends Enum<S>, C extends Enum<C>> {

    void onEvent(GoalSelectionEvent<T, S, C> event);
}
