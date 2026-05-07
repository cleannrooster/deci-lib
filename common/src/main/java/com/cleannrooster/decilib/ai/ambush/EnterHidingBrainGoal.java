package com.cleannrooster.decilib.ai.ambush;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

public class EnterHidingBrainGoal<E extends MobEntity & CanAmbush> implements MobBrainGoal<E> {

    private final int rehideDelayTicks;

    @Nullable private GoalEffects<E> effects;

    private int     delayRemaining;
    private boolean done;

    EnterHidingBrainGoal(int rehideDelayTicks) {
        this.rehideDelayTicks = rehideDelayTicks;
    }

    public EnterHidingBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    @Override
    public boolean isCommitted() {
        return !done;
    }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return true;
    }

    @Override
    public void reset(E entity) {
        delayRemaining = rehideDelayTicks;
        done           = false;
    }

    @Override
    public void start(E entity, ServerWorld world) {
        reset(entity);
        entity.getNavigation().stop();
        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);

        delayRemaining--;
        if (delayRemaining <= 0 && !done) {
            entity.enterHiddenState();
            if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);
            done = true;
        }
    }

    @Override
    public boolean shouldContinue(E entity, AIStimulus stimulus) {
        return !done;
    }

    @Override
    public void stop(E entity, ServerWorld world) {
        if (!done && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
        reset(entity);
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason) {
        if (reason == StopReason.SHUTDOWN && !done) {
            entity.enterHiddenState();
        }
        if (!done && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
        reset(entity);
    }

    @Override
    public int expectedDurationTicks() {
        return rehideDelayTicks;
    }
}
