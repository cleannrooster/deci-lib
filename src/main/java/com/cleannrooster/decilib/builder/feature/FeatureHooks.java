package com.cleannrooster.decilib.builder.feature;

import com.cleannrooster.decilib.ai.goal.GoalEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;


public record FeatureHooks(Map<FeatureLifecycleEvent, List<LifecycleEffect>> effects) {

    public void fireIfPresent(FeatureLifecycleEvent event,
                              LivingEntity entity,
                              @Nullable ServerWorld world) {
        List<LifecycleEffect> list = effects.get(event);
        if (list != null) list.forEach(e -> e.fire(entity, world));
    }


    @SuppressWarnings("unchecked")
    public <E extends LivingEntity> GoalEffects<E> toGoalEffects() {
        return (GoalEffects<E>) GoalEffects.<LivingEntity>builder()
                .onStart((e, w)     -> fireIfPresent(FeatureLifecycleEvent.START,     e, w))
                .onAction((e, w)    -> fireIfPresent(FeatureLifecycleEvent.ON_ACTION, e, w))
                .onComplete((e, w)  -> fireIfPresent(FeatureLifecycleEvent.COMPLETE,  e, w))
                .onInterrupt((e, w) -> fireIfPresent(FeatureLifecycleEvent.CANCEL,    e, w))
                .build();
    }
}
