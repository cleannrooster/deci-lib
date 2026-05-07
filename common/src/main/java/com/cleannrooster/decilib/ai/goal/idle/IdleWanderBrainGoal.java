package com.cleannrooster.decilib.ai.goal.idle;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class IdleWanderBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    private final double wanderRange;
    private final double speed;
    private final int    pauseTicks;

    @Nullable private Vec3d  homeCenter;
    private           double homeRadius   = 0.0;
    private           int    scanInterval = 0;

    private int     pauseRemaining;
    private boolean moving;
    private int     scanTick;

    @Nullable private GoalEffects<E> effects;

    public IdleWanderBrainGoal(double wanderRange, double speed, int pauseTicks) {
        this.wanderRange = wanderRange;
        this.speed       = speed;
        this.pauseTicks  = pauseTicks;
    }


    public IdleWanderBrainGoal<E> withHome(Vec3d center, double radius) {
        this.homeCenter = center;
        this.homeRadius = radius;
        return this;
    }


    public IdleWanderBrainGoal<E> withScan(int intervalTicks) {
        this.scanInterval = intervalTicks;
        return this;
    }

    public IdleWanderBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return !stimulus.hasTarget();
    }

    @Override
    public void reset(E entity) {
        pauseRemaining = 0;
        moving         = false;
        scanTick       = 0;
    }

    @Override
    public void start(E entity, ServerWorld world) {
        reset(entity);
        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
        startWander(entity);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);

        if (moving) {
            if (!entity.getNavigation().isFollowingPath()) {
                moving         = false;
                pauseRemaining = pauseTicks;
            }
        } else {
            pauseRemaining--;
            if (scanInterval > 0) {
                scanTick++;
                if (scanTick % scanInterval == 0) {
                    var yaw = entity.getRandom().nextFloat() * 360f;
                    entity.setHeadYaw(yaw);
                    entity.bodyYaw = yaw;
                }
            }
            if (pauseRemaining <= 0) {
                startWander(entity);
            }
        }
    }

    @Override
    public boolean shouldContinue(E entity, AIStimulus stimulus) {
        return !stimulus.hasTarget();
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

    private void startWander(E entity) {
        var  base  = homeCenter != null ? homeCenter : entity.getPos();
        var  range = homeCenter != null ? Math.min(wanderRange, homeRadius) : wanderRange;
        var  dx    = (entity.getRandom().nextDouble() * 2 - 1) * range;
        var  dz    = (entity.getRandom().nextDouble() * 2 - 1) * range;
        entity.getNavigation().startMovingTo(base.x + dx, base.y, base.z + dz, speed);
        moving = true;
    }
}
