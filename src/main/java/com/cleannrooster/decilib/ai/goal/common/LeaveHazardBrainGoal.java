package com.cleannrooster.decilib.ai.goal.common;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class LeaveHazardBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    public enum EscapeMode    { TELEPORT, LAUNCH }
    public enum ConditionMode { ANY, ALL }

    // ── Config ────────────────────────────────────────────────────────────────

    private final String        abilityId;
    private final int           cooldownTicks;

    // Hazard checks
    private final boolean       checkInWater;
    private final boolean       checkInLava;
    private final boolean       checkSustainedDamage;
    private final float         sustainedDamageThreshold;
    private final int           sustainedDamageInteractionTicks;
    private final ConditionMode conditionMode;

    // Escape
    private final EscapeMode    escapeMode;
    private final double        teleportRadius;
    private final double        teleportMinRadius;
    private final int           teleportAttempts;
    private final double        launchVerticalSpeed;
    private final double        launchHorizontalSpeed;

    @Nullable private GoalEffects<E> effects;

    // ── Per-activation state ─────────────────────────────────────────────────

    private boolean done;

    // ── Constructor ──────────────────────────────────────────────────────────

    public LeaveHazardBrainGoal(String abilityId, int cooldownTicks,
                                 boolean checkInWater, boolean checkInLava,
                                 boolean checkSustainedDamage, float sustainedDamageThreshold,
                                 int sustainedDamageInteractionTicks, ConditionMode conditionMode,
                                 EscapeMode escapeMode,
                                 double teleportRadius, double teleportMinRadius, int teleportAttempts,
                                 double launchVerticalSpeed, double launchHorizontalSpeed) {
        this.abilityId                       = abilityId;
        this.cooldownTicks                   = cooldownTicks;
        this.checkInWater                    = checkInWater;
        this.checkInLava                     = checkInLava;
        this.checkSustainedDamage            = checkSustainedDamage;
        this.sustainedDamageThreshold        = sustainedDamageThreshold;
        this.sustainedDamageInteractionTicks = sustainedDamageInteractionTicks;
        this.conditionMode                   = conditionMode;
        this.escapeMode                      = escapeMode;
        this.teleportRadius                  = teleportRadius;
        this.teleportMinRadius               = Math.min(teleportMinRadius, teleportRadius);
        this.teleportAttempts                = teleportAttempts;
        this.launchVerticalSpeed             = launchVerticalSpeed;
        this.launchHorizontalSpeed           = launchHorizontalSpeed;
    }

    public LeaveHazardBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    // ── MobBrainGoal ─────────────────────────────────────────────────────────

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.hasTarget() && cooldowns.isReady(abilityId) && isHazard(stimulus);
    }

    @Override
    public void reset(E entity) {
        done = false;
    }

    @Override
    public void start(E entity, ServerWorld world) {
        reset(entity);
        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        LivingEntity target = entity.getTarget();
        boolean escaped = false;

        if (target != null) {
            escaped = switch (escapeMode) {
                case TELEPORT -> performTeleport(entity, target, world);
                case LAUNCH   -> { performLaunch(entity, target); yield true; }
            };
        }

        if (escaped && effects != null && effects.onAction() != null) effects.onAction().accept(entity, world);
        cooldowns.trigger(abilityId, cooldownTicks);
        if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);
        done = true;
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
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason) {
        if (reason != StopReason.SHUTDOWN && !done
                && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
    }

    @Override
    public Set<String> declaredAbilityIds() { return Set.of(abilityId); }

    // ── Hazard detection ─────────────────────────────────────────────────────

    private boolean isHazard(AIStimulus stimulus) {
        if (!checkInWater && !checkInLava && !checkSustainedDamage) return false;

        if (conditionMode == ConditionMode.ALL) {
            if (checkInWater         && !stimulus.selfIsInWater())            return false;
            if (checkInLava          && !stimulus.selfIsInLava())             return false;
            if (checkSustainedDamage && !sustainedDamageCondition(stimulus))  return false;
            return true;
        } else {
            if (checkInWater         && stimulus.selfIsInWater())             return true;
            if (checkInLava          && stimulus.selfIsInLava())              return true;
            if (checkSustainedDamage && sustainedDamageCondition(stimulus))   return true;
            return false;
        }
    }

    private boolean sustainedDamageCondition(AIStimulus stimulus) {
        return stimulus.recentDamageTaken() >= sustainedDamageThreshold
                && stimulus.ticksSinceLastHit() >= sustainedDamageInteractionTicks;
    }

    // ── Escape actions ───────────────────────────────────────────────────────

    private boolean performTeleport(E entity, LivingEntity target, ServerWorld world) {
        double range = Math.max(0, teleportRadius - teleportMinRadius);
        for (int i = 0; i < teleportAttempts; i++) {
            double angle = world.random.nextDouble() * 2 * Math.PI;
            double dist  = teleportMinRadius + world.random.nextDouble() * range;
            double x = target.getX() + Math.cos(angle) * dist;
            double z = target.getZ() + Math.sin(angle) * dist;
            BlockPos pos = BlockPos.ofFloored(x, target.getY(), z);
            if (isValidTeleportPos(world, pos)) {
                entity.teleport(x, target.getY(), z);
                entity.getNavigation().stop();
                return true;
            }
        }
        return false;
    }

    private void performLaunch(E entity, LivingEntity target) {
        Vec3d toTarget = target.getPos().subtract(entity.getPos());
        double horizLen = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        double dx = horizLen > 0.001 ? (toTarget.x / horizLen) * launchHorizontalSpeed : 0;
        double dz = horizLen > 0.001 ? (toTarget.z / horizLen) * launchHorizontalSpeed : 0;
        entity.setVelocity(dx, launchVerticalSpeed, dz);
        entity.velocityModified = true;
        entity.getNavigation().stop();
    }

    private boolean isValidTeleportPos(ServerWorld world, BlockPos pos) {
        BlockState feet = world.getBlockState(pos);
        BlockState head = world.getBlockState(pos.up());
        return !feet.blocksMovement() && !feet.isLiquid()
                && !head.blocksMovement() && !head.isLiquid();
    }
}
