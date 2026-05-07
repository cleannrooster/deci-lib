package com.cleannrooster.decilib.ai.goal.common;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import com.cleannrooster.decilib.ai.util.DamageUtil;
import com.cleannrooster.decilib.builder.animation.MobAnimationDispatcherRegistry;
import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class FrenzyBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    public enum MovementMode { COMMIT, TURN_THEN_COMMIT, TRACK }
    public enum AttackType   { ARC, RADIAL }

    private final String       abilityId;
    private final int          cooldownTicks;
    private final int          durationTicks;
    private final double       speed;
    private final MovementMode movementMode;
    private final AttackType   attackType;
    private final int          attackIntervalTicks;
    private final double       range;
    private final float        halfAngleDeg;
    private final float        damage;
    private final float        coeff;
    private final int          turnTicks;

    @Nullable private String         frenzyLoopAnim;
    @Nullable private GoalEffects<E> effects;

    // per-activation state
    private int     ticksElapsed;
    private boolean completed;
    @Nullable
    private Vec3d   lockedDirection;
    private boolean directionLocked;

    public FrenzyBrainGoal(String abilityId, int cooldownTicks, int durationTicks, double speed,
                            MovementMode movementMode, AttackType attackType,
                            int attackIntervalTicks, double range, float halfAngleDeg,
                            float damage, float coeff, int turnTicks) {
        this.abilityId           = abilityId;
        this.cooldownTicks       = cooldownTicks;
        this.durationTicks       = durationTicks;
        this.speed               = speed;
        this.movementMode        = movementMode;
        this.attackType          = attackType;
        this.attackIntervalTicks = attackIntervalTicks;
        this.range               = range;
        this.halfAngleDeg        = halfAngleDeg;
        this.damage              = damage;
        this.coeff               = coeff;
        this.turnTicks           = turnTicks;
    }

    public FrenzyBrainGoal<E> frenzyLoopAnim(@Nullable String anim) {
        this.frenzyLoopAnim = (anim != null && !anim.isBlank()) ? anim : null;
        return this;
    }

    public FrenzyBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    // ── MobBrainGoal ─────────────────────────────────────────────────────────

    @Override
    public boolean isCommitted() { return !completed; }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.hasTarget() && cooldowns.isReady(abilityId);
    }

    @Override
    public void reset(E entity) {
        ticksElapsed    = 0;
        completed       = false;
        lockedDirection = null;
        directionLocked = false;
    }

    @Override
    public void start(E entity, ServerWorld world) {
        reset(entity);

        if (movementMode == MovementMode.COMMIT) {
            lockDirection(entity);
        }

        if (frenzyLoopAnim != null && entity instanceof DataDrivenMob ddm) {
            ddm.getLoopAnimTracker().suppressFor(durationTicks + 5);
            MobAnimationDispatcherRegistry.get().triggerLoop(ddm, frenzyLoopAnim);
        }

        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        ticksElapsed++;

        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);

        handleMovement(entity);

        if (ticksElapsed % attackIntervalTicks == 0) {
            applyDamage(entity, world);
            if (effects != null && effects.onAction() != null) effects.onAction().accept(entity, world);
        }

        if (ticksElapsed >= durationTicks) {
            if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);
            cooldowns.trigger(abilityId, cooldownTicks);
            completed = true;
        }
    }

    @Override
    public boolean shouldContinue(E entity, AIStimulus stimulus) {
        return !completed;
    }

    @Override
    public void stop(E entity, ServerWorld world) {
        if (!completed && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
        stopFrenzyAnimation(entity);
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason) {
        if (reason != StopReason.SHUTDOWN && !completed
                && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
        stopFrenzyAnimation(entity);
    }

    @Override
    public int expectedDurationTicks() { return durationTicks; }

    @Override
    public Set<String> declaredAbilityIds() { return Set.of(abilityId); }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void handleMovement(E entity) {
        switch (movementMode) {
            case TRACK -> {
                var target = entity.getTarget();
                if (target != null) entity.getNavigation().startMovingTo(target, speed);
            }
            case COMMIT -> {
                if (directionLocked) moveInLockedDirection(entity);
            }
            case TURN_THEN_COMMIT -> {
                if (ticksElapsed <= turnTicks) {
                    var target = entity.getTarget();
                    if (target != null) {
                        entity.getLookControl().lookAt(target, 30f, 30f);
                        entity.getNavigation().startMovingTo(target, speed);
                    }
                } else {
                    if (!directionLocked) lockDirection(entity);
                    moveInLockedDirection(entity);
                }
            }
        }
    }

    private void lockDirection(E entity) {
        lockedDirection = entity.getRotationVec(1.0f).normalize();
        directionLocked = true;
    }

    private void moveInLockedDirection(E entity) {
        if (lockedDirection == null) return;
        var dest = entity.getPos().add(lockedDirection.multiply(64.0));
        entity.getNavigation().startMovingTo(dest.x, dest.y, dest.z, speed);
    }

    private void applyDamage(E entity, ServerWorld world) {
        float totalDmg = damage + coeff * (float) entity.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        var source = world.getDamageSources().mobAttack(entity);
        if (attackType == AttackType.ARC) {
            DamageUtil.performArcDamage(entity, entity.getTarget(), halfAngleDeg, range, totalDmg, source, world);
        } else {
            DamageUtil.performRadialDamage(entity, range, totalDmg, source, world);
        }
    }

    private void stopFrenzyAnimation(E entity) {
        if (frenzyLoopAnim != null && entity instanceof DataDrivenMob ddm) {
            ddm.getLoopAnimTracker().reset();
        }
    }
}
