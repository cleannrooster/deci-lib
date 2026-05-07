package com.cleannrooster.decilib.ai.goal.idle;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

public class IdleStillBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    private int scanInterval = 0;
    private int tickCount;

    @Nullable private GoalEffects<E> effects;


    public IdleStillBrainGoal<E> withScan(int intervalTicks) {
        this.scanInterval = intervalTicks;
        return this;
    }

    public IdleStillBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return !stimulus.hasTarget();
    }

    @Override
    public void reset(E entity) {
        tickCount = 0;
    }

    @Override
    public void start(E entity, ServerWorld world) {
        reset(entity);
        entity.getNavigation().stop();
        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        tickCount++;
        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);

        if (scanInterval > 0 && tickCount % scanInterval == 0) {
            var yaw = entity.getRandom().nextFloat() * 360f;
            entity.setHeadYaw(yaw);
            entity.bodyYaw = yaw;
        }
    }

    @Override
    public boolean shouldContinue(E entity, AIStimulus stimulus) {
        return !stimulus.hasTarget();
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
