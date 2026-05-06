package com.cleannrooster.decilib.ai.goal.common;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class RepositionBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    public enum Movement { PATHFIND, DASH }
    public enum SpeedMode { ABSOLUTE, DYNAMIC }
    public enum Anchor    { SELF, TARGET }

    private static final double MAX_NAV_SPEED = 5.0;

    // ── Config ────────────────────────────────────────────────────────────────

    private final String    abilityId;
    private final int       cooldownTicks;
    private final int       durationTicks;
    private final double    radius;
    private final double    minDistance;
    private final double    arrivalRadius;
    private final Movement  movement;
    private final SpeedMode speedMode;
    private final double    speed;
    private final Anchor    anchor;

    @Nullable private GoalEffects<E> effects;

    // ── Per-activation state ─────────────────────────────────────────────────

    @Nullable private Vec3d  destination;
    private           double appliedSpeed;
    private           Vec3d  dashDir;
    private           int    ticksElapsed;
    private           boolean done;

    // ── Constructor ──────────────────────────────────────────────────────────

    public RepositionBrainGoal(String abilityId, int cooldownTicks, int durationTicks,
                                double radius, double minDistance, double arrivalRadius,
                                Movement movement, SpeedMode speedMode, double speed,
                                Anchor anchor) {
        this.abilityId     = abilityId;
        this.cooldownTicks = cooldownTicks;
        this.durationTicks = durationTicks;
        this.radius        = radius;
        this.minDistance   = Math.min(minDistance, radius);
        this.arrivalRadius = arrivalRadius;
        this.movement      = movement;
        this.speedMode     = speedMode;
        this.speed         = speed;
        this.anchor        = anchor;
    }

    public RepositionBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    // ── MobBrainGoal ─────────────────────────────────────────────────────────

    @Override
    public boolean isCommitted() {
        // DASH commits the mob to its current state until it arrives or times out.
        // PATHFIND allows the state machine to preempt with higher-priority goals.
        return movement == Movement.DASH && !done;
    }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.hasTarget() && cooldowns.isReady(abilityId);
    }

    @Override
    public void reset(E entity) {
        destination  = null;
        appliedSpeed = 0;
        dashDir      = Vec3d.ZERO;
        ticksElapsed = 0;
        done         = false;
    }

    @Override
    public void start(E entity, ServerWorld world) {
        reset(entity);

        destination = pickDestination(entity, world);

        double horizDist = horizontalDist(entity.getPos(), destination);

        appliedSpeed = resolveSpeed(entity, horizDist);

        if (movement == Movement.DASH) {
            dashDir = horizontalDirection(entity.getPos(), destination);
            entity.getNavigation().stop();
        } else {
            entity.getNavigation().startMovingTo(destination.x, destination.y, destination.z, appliedSpeed);
        }

        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        ticksElapsed++;
        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);

        if (destination == null) { done = true; return; }

        boolean arrived   = entity.squaredDistanceTo(destination) < arrivalRadius * arrivalRadius;
        boolean timedOut  = ticksElapsed >= durationTicks;

        if (arrived || timedOut) {
            if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);
            cooldowns.trigger(abilityId, cooldownTicks);
            done = true;
            return;
        }

        if (movement == Movement.DASH) {
            Vec3d vel = entity.getVelocity();
            entity.setVelocity(dashDir.x * appliedSpeed, vel.y, dashDir.z * appliedSpeed);
            entity.velocityModified = true;
        } else {
            entity.getNavigation().startMovingTo(destination.x, destination.y, destination.z, appliedSpeed);
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
        if (movement == Movement.DASH) entity.setVelocity(0, entity.getVelocity().y, 0);
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason) {
        if (reason != StopReason.SHUTDOWN && !done
                && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
        if (movement == Movement.DASH) entity.setVelocity(0, entity.getVelocity().y, 0);
    }

    @Override
    public int expectedDurationTicks() { return durationTicks; }

    @Override
    public Set<String> declaredAbilityIds() { return Set.of(abilityId); }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Vec3d pickDestination(E entity, ServerWorld world) {
        Vec3d center = anchor == Anchor.TARGET && entity.getTarget() != null
                ? entity.getTarget().getPos()
                : entity.getPos();

        double range = Math.max(0, radius - minDistance);
        double dist  = minDistance + world.random.nextDouble() * range;
        double angle = world.random.nextDouble() * 2 * Math.PI;

        return new Vec3d(
                center.x + Math.cos(angle) * dist,
                entity.getY(),
                center.z + Math.sin(angle) * dist
        );
    }

    private double resolveSpeed(E entity, double horizDist) {
        if (speedMode == SpeedMode.ABSOLUTE) return speed;

        // DYNAMIC: compute the speed needed to cover the distance in durationTicks
        if (movement == Movement.DASH) {
            return durationTicks > 0 ? horizDist / durationTicks : speed;
        } else {
            double baseSpeed = entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (baseSpeed <= 0 || durationTicks <= 0) return speed;
            return Math.min(horizDist / (durationTicks * baseSpeed), MAX_NAV_SPEED);
        }
    }

    private static double horizontalDist(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static Vec3d horizontalDirection(Vec3d from, Vec3d to) {
        double dx  = to.x - from.x;
        double dz  = to.z - from.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        return len > 0.001 ? new Vec3d(dx / len, 0, dz / len) : Vec3d.ZERO;
    }
}
