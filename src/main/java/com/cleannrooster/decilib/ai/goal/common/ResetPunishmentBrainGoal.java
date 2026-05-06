package com.cleannrooster.decilib.ai.goal.common;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class ResetPunishmentBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    private enum Phase { TRACKING, WINDING_UP, COMPLETE }

    private final String abilityId;
    private final int    cooldownTicks;
    private final int    interruptCooldownTicks;
    private final double rangeThreshold;
    private final int    timeAtRangeTicks;
    private final int    windupTicks;
    private final double interruptRange;
    private final float  healAmount;
    private final float  healFraction;
    private final double approachSpeed;

    @Nullable private GoalEffects<E> effects;

    private Phase phase;
    private int   ticksAtRange;
    private int   windupRemaining;

    public ResetPunishmentBrainGoal(String abilityId, int cooldownTicks, int interruptCooldownTicks,
                                     double rangeThreshold, int timeAtRangeTicks, int windupTicks,
                                     double interruptRange, float healAmount, float healFraction,
                                     double approachSpeed) {
        this.abilityId              = abilityId;
        this.cooldownTicks          = cooldownTicks;
        this.interruptCooldownTicks = interruptCooldownTicks;
        this.rangeThreshold         = rangeThreshold;
        this.timeAtRangeTicks       = timeAtRangeTicks;
        this.windupTicks            = windupTicks;
        this.interruptRange         = interruptRange;
        this.healAmount             = healAmount;
        this.healFraction           = healFraction;
        this.approachSpeed          = approachSpeed;
    }

    public ResetPunishmentBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    @Override
    public boolean isCommitted() { return phase == Phase.WINDING_UP; }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.hasTarget()
                && stimulus.targetDistance() > rangeThreshold
                && cooldowns.isReady(abilityId);
    }

    @Override
    public void reset(E entity) {
        phase           = Phase.TRACKING;
        ticksAtRange    = 0;
        windupRemaining = windupTicks;
    }

    @Override
    public void start(E entity, ServerWorld world) {
        reset(entity);
        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        switch (phase) {
            case TRACKING  -> tickTracking(entity, world, stimulus);
            case WINDING_UP -> tickWindup(entity, world, stimulus, cooldowns);
            case COMPLETE  -> {}
        }
    }

    private void tickTracking(E entity, ServerWorld world, AIStimulus stimulus) {
        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);

        // Keep approaching while counting time at range
        var target = entity.getTarget();
        if (target != null) {
            entity.getLookControl().lookAt(target, 30f, 30f);
            entity.getNavigation().startMovingTo(target, approachSpeed);
        }

        if (stimulus.targetDistance() > rangeThreshold) {
            ticksAtRange++;
            if (ticksAtRange >= timeAtRangeTicks) {
                phase           = Phase.WINDING_UP;
                windupRemaining = windupTicks;
                entity.getNavigation().stop();
            }
        } else {
            ticksAtRange = 0;
        }
    }

    private void tickWindup(E entity, ServerWorld world, AIStimulus stimulus,
                             AbilityCooldownRegistry cooldowns) {
        windupRemaining--;

        // Telegraphed windup particles
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                entity.getX(), entity.getY() + entity.getHeight() * 0.8, entity.getZ(),
                3, 0.4, 0.2, 0.4, 0.1);

        // Interrupt if player closes in
        if (stimulus.targetDistance() <= interruptRange) {
            cooldowns.trigger(abilityId, interruptCooldownTicks);
            if (effects != null && effects.onInterrupt() != null) effects.onInterrupt().accept(entity, world);
            phase = Phase.COMPLETE;
            return;
        }

        if (windupRemaining <= 0) {
            float toHeal = entity.getMaxHealth() * healFraction + healAmount;
            entity.heal(toHeal);
            if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);
            cooldowns.trigger(abilityId, cooldownTicks);
            phase = Phase.COMPLETE;
        }
    }

    @Override
    public boolean shouldContinue(E entity, AIStimulus stimulus) {
        return phase != Phase.COMPLETE && stimulus.hasTarget();
    }

    @Override
    public void stop(E entity, ServerWorld world) {
        if (phase == Phase.TRACKING && effects != null && effects.onInterrupt() != null) {
            // quiet exit during tracking — no interrupt effect
        } else if (phase == Phase.WINDING_UP && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
        phase = Phase.COMPLETE;
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason) {
        if (reason != StopReason.SHUTDOWN && phase == Phase.WINDING_UP
                && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
        phase = Phase.COMPLETE;
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason, AbilityCooldownRegistry cooldowns) {
        if (reason != StopReason.SHUTDOWN && phase == Phase.WINDING_UP) {
            if (effects != null && effects.onInterrupt() != null) effects.onInterrupt().accept(entity, world);
            cooldowns.trigger(abilityId, interruptCooldownTicks);
        }
        phase = Phase.COMPLETE;
    }

    @Override
    public int expectedDurationTicks() { return timeAtRangeTicks + windupTicks; }

    @Override
    public Set<String> declaredAbilityIds() { return Set.of(abilityId); }
}
