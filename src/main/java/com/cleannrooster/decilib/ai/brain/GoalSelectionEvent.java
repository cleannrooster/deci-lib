package com.cleannrooster.decilib.ai.brain;

import com.cleannrooster.decilib.ai.goal.GoalBinding;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public record GoalSelectionEvent<T extends LivingEntity, S extends Enum<S>, C extends Enum<C>>(
        GoalBinding<T, S, C> binding,
        @Nullable GoalSkipReason skipReason
) {
    public boolean wasSelected() { return skipReason == null; }
}
