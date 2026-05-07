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

import java.util.function.Predicate;

public class ApproachTargetBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    private final double speed;
    private double targetYOffset = 0.0;

    /** Distance at which the mob holds when {@link #closeInGate} fails. <=0 means no standoff. */
    private double standoffRange = 0.0;

    /**
     * When non-null and failing, the mob stops at {@link #standoffRange} instead of
     * closing to melee. When passing (or when null), the mob closes all the way.
     */
    @Nullable private Predicate<AIStimulus> closeInGate = null;

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

    /**
     * Configures a standoff distance. When {@code gate} fails the mob will
     * approach to {@code range} blocks and then hold position rather than
     * closing all the way to melee range.
     */
    public ApproachTargetBrainGoal<E> withStandoff(double range, Predicate<AIStimulus> gate) {
        this.standoffRange = range;
        this.closeInGate   = gate;
        return this;
    }

    private boolean wantsToCloseIn(AIStimulus stimulus) {
        return closeInGate == null || closeInGate.test(stimulus);
    }

    private boolean atStandoff(AIStimulus stimulus) {
        return standoffRange > 0 && stimulus.targetDistance() <= standoffRange;
    }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        if (!stimulus.hasTarget()) return false;
        if (wantsToCloseIn(stimulus)) return !stimulus.targetInMeleeRange();
        // Conditions unfavorable — only start if we're not yet at the standoff distance.
        return !atStandoff(stimulus);
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
        if (!stimulus.hasTarget()) return false;
        if (wantsToCloseIn(stimulus)) return !stimulus.targetInMeleeRange();
        // Conditions unfavorable — hold at standoff. Stop goal so the mob idles in place.
        return !atStandoff(stimulus);
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
