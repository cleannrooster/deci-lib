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

import java.util.Set;

public class ChaseBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    private final String abilityId;
    private final int    cooldownTicks;
    private final double chaseSpeed;
    private final double engageRange;
    private final int    idleAttackWindow;
    private final int    maxDurationTicks;

    @Nullable private GoalEffects<E> effects;

    private int  ticksElapsed;
    private boolean completed;

    public ChaseBrainGoal(String abilityId, int cooldownTicks, double chaseSpeed,
                           double engageRange, int idleAttackWindow, int maxDurationTicks) {
        this.abilityId        = abilityId;
        this.cooldownTicks    = cooldownTicks;
        this.chaseSpeed       = chaseSpeed;
        this.engageRange      = engageRange;
        this.idleAttackWindow = idleAttackWindow;
        this.maxDurationTicks = maxDurationTicks;
    }

    public ChaseBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.hasTarget()
                && stimulus.targetDistance() > engageRange
                && stimulus.ticksSinceLastAttack() > idleAttackWindow
                && cooldowns.isReady(abilityId);
    }

    @Override
    public void reset(E entity) {
        ticksElapsed = 0;
        completed    = false;
    }

    @Override
    public void start(E entity, ServerWorld world) {
        reset(entity);
        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        ticksElapsed++;
        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);

        var target = entity.getTarget();
        if (target != null) {
            entity.getLookControl().lookAt(target, 30f, 30f);
            entity.getNavigation().startMovingTo(target, chaseSpeed);
        }

        if (stimulus.targetInMeleeRange() || ticksElapsed >= maxDurationTicks) {
            if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);
            cooldowns.trigger(abilityId, cooldownTicks);
            completed = true;
        }
    }

    @Override
    public boolean shouldContinue(E entity, AIStimulus stimulus) {
        return !completed && stimulus.hasTarget();
    }

    @Override
    public void stop(E entity, ServerWorld world) {
        if (!completed && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason) {
        if (reason != StopReason.SHUTDOWN && !completed
                && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
    }

    @Override
    public int expectedDurationTicks() { return maxDurationTicks; }

    @Override
    public Set<String> declaredAbilityIds() { return Set.of(abilityId); }
}
