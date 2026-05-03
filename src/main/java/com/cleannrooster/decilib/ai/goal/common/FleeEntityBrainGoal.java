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
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class FleeEntityBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    private final double detectionRange;
    private final double stopRange;      // wider than detectionRange to prevent boundary oscillation
    private final double fleeSpeed;

    @Nullable private GoalEffects<E> effects;


    public FleeEntityBrainGoal(double detectionRange, double fleeSpeed) {
        this.detectionRange = detectionRange;
        this.stopRange      = detectionRange * 1.25;
        this.fleeSpeed      = fleeSpeed;
    }

    public FleeEntityBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.hasTarget() && stimulus.targetDistance() < detectionRange;
    }

    @Override
    public void start(E entity, ServerWorld world) {
        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
        navigateAway(entity);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);
        navigateAway(entity);
    }

    @Override
    public boolean shouldContinue(E entity, AIStimulus stimulus) {
        return stimulus.hasTarget() && stimulus.targetDistance() < stopRange;
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

    private void navigateAway(E entity) {
        var threat = entity.getTarget();
        if (threat == null) return;
        var away = entity.getPos().subtract(threat.getPos()).normalize();
        var dest = entity.getPos().add(away.multiply(detectionRange));
        entity.getNavigation().startMovingTo(dest.x, dest.y, dest.z, fleeSpeed);
    }
}
