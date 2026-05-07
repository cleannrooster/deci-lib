package com.cleannrooster.decilib.ai.goal.common;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

public class SwimBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    @Nullable private GoalEffects<E> effects;

    public SwimBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.selfIsInFluid();
    }

    @Override
    public void start(E entity, ServerWorld world) {
        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);
        entity.getJumpControl().setActive();
    }

    @Override
    public boolean shouldContinue(E entity, AIStimulus stimulus) {
        return stimulus.selfIsInFluid();
    }

    @Override
    public void stop(E entity, ServerWorld world) {
        if (effects != null && effects.onInterrupt() != null) effects.onInterrupt().accept(entity, world);
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason) {
        if (effects != null) {
            if (reason == StopReason.COMPLETED && effects.onComplete() != null) {
                effects.onComplete().accept(entity, world);
            } else if (reason != StopReason.COMPLETED && effects.onInterrupt() != null) {
                effects.onInterrupt().accept(entity, world);
            }
        }
    }
}
