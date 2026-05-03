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

public class WatchFromHidingBrainGoal<E extends MobEntity & CanAmbush> implements MobBrainGoal<E> {
    private final double           detectionRange;
    private final int              scanIntervalTicks;
    private final ThreatScanner<E> scanner;
    @Nullable private GoalEffects<E> effects;
    private int tickCount;
    WatchFromHidingBrainGoal(AmbushConfig<E> config) {
        this.detectionRange    = config.detectionRange();
        this.scanIntervalTicks = config.scanIntervalTicks();
        this.scanner           = config.scanner();
    }
    public WatchFromHidingBrainGoal<E> withEffects(GoalEffects<E> effects) {
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
        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
    }
    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        tickCount++;
        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);

        if (scanIntervalTicks > 0 && tickCount % scanIntervalTicks == 0) {
            var threat = scanner.scan(entity, world, detectionRange);
            if (threat != null) {
                entity.setTarget(threat);
                // shouldContinue will return false next tick once stimulus reflects the target
            }
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
