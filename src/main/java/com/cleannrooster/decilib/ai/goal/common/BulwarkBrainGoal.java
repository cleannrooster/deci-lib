package com.cleannrooster.decilib.ai.goal.common;

import com.cleannrooster.decilib.ai.CanBulwark;
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

public class BulwarkBrainGoal<E extends MobEntity & CanBulwark> implements MobBrainGoal<E> {

    private final String abilityId;
    private final int    cooldownTicks;
    private final double activationRange;
    private final double deactivationRange;
    private final float  reflectCoeff;
    private final double approachSpeed;

    @Nullable private GoalEffects<E> effects;

    public BulwarkBrainGoal(String abilityId, int cooldownTicks,
                             double activationRange, double deactivationRange,
                             float reflectCoeff, double approachSpeed) {
        this.abilityId         = abilityId;
        this.cooldownTicks     = cooldownTicks;
        this.activationRange   = activationRange;
        this.deactivationRange = deactivationRange;
        this.reflectCoeff      = reflectCoeff;
        this.approachSpeed     = approachSpeed;
    }

    public BulwarkBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.hasTarget()
                && stimulus.targetDistance() < activationRange
                && cooldowns.isReady(abilityId);
    }

    @Override
    public void start(E entity, ServerWorld world) {
        entity.enterBulwark(reflectCoeff);
        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);

        var target = entity.getTarget();
        if (target != null) {
            entity.getLookControl().lookAt(target, 30f, 30f);
            entity.getNavigation().startMovingTo(target, approachSpeed);
        }
    }

    @Override
    public boolean shouldContinue(E entity, AIStimulus stimulus) {
        return stimulus.hasTarget() && stimulus.targetDistance() < deactivationRange;
    }

    @Override
    public void stop(E entity, ServerWorld world) {
        entity.exitBulwark();
        if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason) {
        entity.exitBulwark();
        if (reason != StopReason.SHUTDOWN) {
            if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);
        }
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason, AbilityCooldownRegistry cooldowns) {
        entity.exitBulwark();
        cooldowns.trigger(abilityId, cooldownTicks);
        if (reason != StopReason.SHUTDOWN
                && effects != null && effects.onComplete() != null) {
            effects.onComplete().accept(entity, world);
        }
    }

    @Override
    public Set<String> declaredAbilityIds() { return Set.of(abilityId); }
}
