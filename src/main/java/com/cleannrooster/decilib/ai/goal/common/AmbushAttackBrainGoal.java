package com.cleannrooster.decilib.ai.goal.common;

import com.cleannrooster.decilib.ai.ambush.CanAmbush;
import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import org.jetbrains.annotations.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

public class AmbushAttackBrainGoal<E extends MobEntity & CanAmbush> implements MobBrainGoal<E> {

    public enum MovementMode { APPROACH, STRAFE }
    public enum SurfaceMode  { BEHIND, FRONT, UNDERNEATH, RANDOM }

    private static final float STRAFE_DELTA = 0.04f; // radians/tick

    private final String       abilityId;
    private final int          cooldownTicks;
    private final int          minCooldownTicks;
    private final int          hiddenDurationTicks;
    private final MovementMode movementMode;
    private final SurfaceMode  surfaceMode;
    private final double       strafeRadius;
    private final boolean      burrowMode;

    @Nullable private GoalEffects<E> effects;

    // per-activation state
    private int         ticksHidden;
    private boolean     surfaced;
    private double      strafeAngle;
    private int         strafeDirection; // +1 or -1
    private SurfaceMode resolvedMode;

    public AmbushAttackBrainGoal(String abilityId, int cooldownTicks, int minCooldownTicks,
                                  int hiddenDurationTicks, MovementMode movementMode,
                                  SurfaceMode surfaceMode, double strafeRadius, boolean burrowMode) {
        this.abilityId           = abilityId;
        this.cooldownTicks       = cooldownTicks;
        this.minCooldownTicks    = minCooldownTicks;
        this.hiddenDurationTicks = hiddenDurationTicks;
        this.movementMode        = movementMode;
        this.surfaceMode         = surfaceMode;
        this.strafeRadius        = strafeRadius;
        this.burrowMode          = burrowMode;
    }

    /** Attaches lifecycle effect callbacks. */
    public AmbushAttackBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    // ── MobBrainGoal ─────────────────────────────────────────────────────────

    @Override
    public boolean isCommitted() { return !surfaced; }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.hasTarget() && cooldowns.isReady(abilityId);
    }

    @Override
    public void reset(E entity) {
        ticksHidden     = 0;
        surfaced        = false;
        strafeAngle     = 0.0;
        strafeDirection = 1;
        resolvedMode    = surfaceMode;
    }

    @Override
    public void start(E entity, ServerWorld world) {
        reset(entity);

        if (surfaceMode == SurfaceMode.RANDOM) {
            SurfaceMode[] choices = { SurfaceMode.BEHIND, SurfaceMode.FRONT, SurfaceMode.UNDERNEATH };
            resolvedMode = choices[world.random.nextInt(choices.length)];
        }

        var target = entity.getTarget();
        if (target != null) {
            var dx = entity.getX() - target.getX();
            var dz = entity.getZ() - target.getZ();
            strafeAngle     = Math.atan2(dx, dz);
            strafeDirection = world.random.nextBoolean() ? 1 : -1;
        }

        entity.enterHiddenState();
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        ticksHidden++;

        if (burrowMode) spawnBurrowParticles(entity, world);
        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);

        var target = entity.getTarget();
        if (target != null) {
            switch (movementMode) {
                case APPROACH -> entity.getNavigation().startMovingTo(target, 1.2);
                case STRAFE -> {
                    strafeAngle += STRAFE_DELTA * strafeDirection;
                    var tx = target.getX() + Math.sin(strafeAngle) * strafeRadius;
                    var tz = target.getZ() + Math.cos(strafeAngle) * strafeRadius;
                    entity.getNavigation().startMovingTo(tx, target.getY(), tz, 1.2);
                }
            }
        }

        if (ticksHidden >= hiddenDurationTicks && !surfaced) {
            var dest = computeSurfacePosition(entity);
            entity.teleport(dest.x, dest.y, dest.z, false);
            entity.exitHiddenState();
            if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);
            cooldowns.trigger(abilityId, scaledCooldown(entity));
            surfaced = true;
        }
    }

    @Override
    public boolean shouldContinue(E entity, AIStimulus stimulus) {
        return !surfaced && stimulus.hasTarget();
    }

    @Override
    public void stop(E entity, ServerWorld world) {
        if (!surfaced) {
            if (entity.isHidden()) entity.exitHiddenState();
            if (effects != null && effects.onInterrupt() != null) effects.onInterrupt().accept(entity, world);
        }
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason) {
        if (reason != StopReason.SHUTDOWN && !surfaced) {
            if (entity.isHidden()) entity.exitHiddenState();
            if (effects != null && effects.onInterrupt() != null) effects.onInterrupt().accept(entity, world);
        }
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason, AbilityCooldownRegistry cooldowns) {
        if (reason != StopReason.SHUTDOWN && !surfaced) {
            if (entity.isHidden()) entity.exitHiddenState();
            if (effects != null && effects.onInterrupt() != null) effects.onInterrupt().accept(entity, world);
        }
        if (!surfaced && reason == StopReason.PREEMPTED) {
            cooldowns.trigger(abilityId, minCooldownTicks);
        }
    }

    @Override
    public Set<String> declaredAbilityIds() { return Set.of(abilityId); }

    @Override
    public int expectedDurationTicks() { return hiddenDurationTicks; }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Vec3d computeSurfacePosition(E entity) {
        var target = entity.getTarget();
        if (target == null) return entity.getPos();
        var targetPos = target.getPos();
        var yawRad   = Math.toRadians(target.getYaw());
        var  forward  = new Vec3d(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
        return switch (resolvedMode) {
            case BEHIND      -> targetPos.subtract(forward.multiply(2.5));
            case FRONT       -> targetPos.add(forward.multiply(2.0));
            case UNDERNEATH  -> targetPos;
            case RANDOM      -> targetPos; // never reached — resolved in start()
        };
    }

    private void spawnBurrowParticles(E entity, ServerWorld world) {
        var below = entity.getBlockPos().down();
        var state = world.getBlockState(below);
        if (!state.isAir()) {
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                    entity.getX(), entity.getY() + 0.1, entity.getZ(),
                    3, 0.3, 0.1, 0.3, 0.0);
        }
        world.spawnParticles(ParticleTypes.LARGE_SMOKE,
                entity.getX(), entity.getY() + 0.1, entity.getZ(),
                2, 0.2, 0.1, 0.2, 0.0);
    }

    protected int scaledCooldown(E entity) { return cooldownTicks; }
}
