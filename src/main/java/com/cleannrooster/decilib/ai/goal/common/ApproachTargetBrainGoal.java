package com.cleannrooster.decilib.ai.goal.common;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

public class ApproachTargetBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    private final double speed;
    private double targetYOffset = 0.0;

    @Nullable private GoalEffects<E> effects;

    public ApproachTargetBrainGoal(double speed) {
        this.speed = speed;
    }
    public ApproachTargetBrainGoal<E> withYOffset(double offset) {
        this.targetYOffset = offset;
        return this;
    }
    public ApproachTargetBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.hasTarget() && !stimulus.targetInMeleeRange();
    }

    @Override
    public void start(E entity, ServerWorld world) {
        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);
        var target = entity.getTarget();
        if (target == null) return;
        entity.getNavigation().startMovingTo(
                target.getX(), target.getY() + targetYOffset, target.getZ(), speed);
    }

    @Override
    public boolean shouldContinue(E entity, AIStimulus stimulus) {
        return stimulus.hasTarget() && !stimulus.targetInMeleeRange();
    }

    @Override
    public void stop(E entity, ServerWorld world) {
        entity.getNavigation().stop();
        if (effects != null && effects.onInterrupt() != null) effects.onInterrupt().accept(entity, world);
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason) {
        entity.getNavigation().stop();
        if (effects != null) {
            if (reason == StopReason.COMPLETED && effects.onComplete() != null) {
                effects.onComplete().accept(entity, world);
            } else if (reason != StopReason.COMPLETED && effects.onInterrupt() != null) {
                effects.onInterrupt().accept(entity, world);
            }
        }
    }
}
