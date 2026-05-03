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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ChargeReleaseBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    private final String                     abilityId;
    private final int                        cooldownTicks;
    private final int                        windupTicks;
    private final int                        interruptCooldownTicks;
    private final boolean                    abortOnTargetLoss;
    private final Consumer<E>                onWindupStart;
    private final Consumer<E>                onWindupEnd;
    private final Consumer<E>                onInterruptHook;
    private final BiConsumer<E, ServerWorld> onRelease;

    @Nullable private final GoalEffects<E>   effects;

    private int     windupRemaining;
    private int     releaseDelay;
    private boolean released;
    private boolean woundUp;

    protected ChargeReleaseBrainGoal(Builder<E> b) {
        this.abilityId              = b.abilityId;
        this.cooldownTicks          = b.cooldownTicks;
        this.windupTicks            = b.windupTicks;
        this.releaseDelay              = b.releaseDelay;
        this.interruptCooldownTicks = b.interruptCooldownTicks;
        this.abortOnTargetLoss      = b.abortOnTargetLoss;
        this.onWindupStart     = b.onWindupStart   != null ? b.onWindupStart   : e -> {};
        this.onWindupEnd       = b.onWindupEnd     != null ? b.onWindupEnd     : e -> {};
        this.onInterruptHook   = b.onInterruptHook != null ? b.onInterruptHook : e -> {};
        this.onRelease         = b.onRelease       != null ? b.onRelease       : (e, w) -> {};
        this.effects           = b.effects;
    }

    @Override
    public boolean isCommitted() {
        return !released;
    }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.hasTarget() && cooldowns.isReady(abilityId);
    }

    @Override
    public void reset(E entity) {
        windupRemaining = windupTicks;
        woundUp = false;
        released = false;
    }

    @Override
    public void start(E entity, ServerWorld world) {
        reset(entity);
        onWindupStart.accept(entity);
        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        windupRemaining--;

        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);
        if(windupRemaining + releaseDelay <= 0 && !released && woundUp) {
            onRelease.accept(entity, world);
            cooldowns.trigger(abilityId, cooldownTicks);
            released = true;
        }
        if (windupRemaining <= 0 && !woundUp) {
            onWindupEnd.accept(entity);
            if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);

            woundUp = true;
        }


    }

    @Override
    public boolean shouldContinue(E entity, AIStimulus stimulus) {
        return !released && (!abortOnTargetLoss || stimulus.hasTarget());
    }

    @Override
    public void stop(E entity, ServerWorld world) {
        if (!released) {
            onInterruptHook.accept(entity);
            if (effects != null && effects.onInterrupt() != null) effects.onInterrupt().accept(entity, world);
        }
        windupRemaining = 0;
        released = false;
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason) {
        if (!released) {
            onInterruptHook.accept(entity);
            if (effects != null && effects.onInterrupt() != null) effects.onInterrupt().accept(entity, world);
        }
        windupRemaining = 0;
        released = false;
    }


    @Override
    public void stop(E entity, ServerWorld world, StopReason reason, AbilityCooldownRegistry cooldowns) {
        if (!released) {
            onInterruptHook.accept(entity);
            if (effects != null && effects.onInterrupt() != null) effects.onInterrupt().accept(entity, world);
            if (reason == StopReason.PREEMPTED && interruptCooldownTicks > 0) {
                cooldowns.trigger(abilityId, interruptCooldownTicks);
            }
        }
        windupRemaining = 0;
        released = false;
    }

    @Override
    public Set<String> declaredAbilityIds() {
        return Set.of(abilityId);
    }

    @Override
    public int expectedDurationTicks() {
        return windupTicks + 1;
    }

    // -------------------------------------------------------------------------

    public static class Builder<E extends MobEntity> {

        private final String abilityId;
        private final int    cooldownTicks;
        private final int    windupTicks;
        private final int    releaseDelay;

        private int                        interruptCooldownTicks;
        private boolean                    abortOnTargetLoss = true;
        private Consumer<E>                onWindupStart;
        private Consumer<E>                onWindupEnd;
        private Consumer<E>                onInterruptHook;
        private BiConsumer<E, ServerWorld> onRelease;
        private GoalEffects<E>             effects;

        /**
         * @param abilityId     unique cooldown key
         * @param cooldownTicks cooldown applied after the release fires
         * @param windupTicks   how many ticks the charge phase lasts
         */
        public Builder(String abilityId, int cooldownTicks, int windupTicks, int releaseDelay) {
            this.abilityId              = abilityId;
            this.cooldownTicks          = cooldownTicks;
            this.windupTicks            = windupTicks;
            this.releaseDelay            = releaseDelay;
            this.interruptCooldownTicks = windupTicks; // default: interrupt costs as long as the windup took
        }

        /**
         * Overrides the cooldown applied when the charge is preempted before releasing.
         * Default is {@code windupTicks}. Set to {@code 0} to disable the interrupt penalty.
         */
        public Builder<E> interruptCooldownTicks(int v)             { this.interruptCooldownTicks = v; return this; }
        public Builder<E> abortOnTargetLoss(boolean v)              { this.abortOnTargetLoss = v;  return this; }
        public Builder<E> onWindupStart(Consumer<E> fn)             { this.onWindupStart    = fn;  return this; }
        public Builder<E> onWindupEnd(Consumer<E> fn)               { this.onWindupEnd      = fn;  return this; }
        public Builder<E> onInterrupt(Consumer<E> fn)               { this.onInterruptHook  = fn;  return this; }
        public Builder<E> onRelease(BiConsumer<E, ServerWorld> fn)  { this.onRelease        = fn;  return this; }
        public Builder<E> effects(GoalEffects<E> effects)           { this.effects          = effects; return this; }

        public ChargeReleaseBrainGoal<E> build() {
            return new ChargeReleaseBrainGoal<>(this);
        }
    }
}
