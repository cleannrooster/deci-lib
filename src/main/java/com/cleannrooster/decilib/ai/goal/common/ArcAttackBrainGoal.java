package com.cleannrooster.decilib.ai.goal.common;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import com.cleannrooster.decilib.ai.util.DamageUtil;
import com.cleannrooster.decilib.ai.util.RangePredicates;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

public class ArcAttackBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    private final String abilityId;
    private final int    cooldownTicks;
    private final int    minCooldownTicks;
    private final float  halfAngleDeg;
    private final double range;
    private final float  damage;
    private final float  scalingFactor;

    @Nullable private GoalEffects<E> effects;

    private boolean done;

    public ArcAttackBrainGoal(String abilityId, int cooldownTicks, int minCooldownTicks,
                               float halfAngleDeg, double range, float damage, float coeff) {
        this.abilityId        = abilityId;
        this.cooldownTicks    = cooldownTicks;
        this.minCooldownTicks = minCooldownTicks;
        this.halfAngleDeg     = halfAngleDeg;
        this.range            = range;
        this.damage           = damage;
        this.scalingFactor    = coeff;
    }

    public ArcAttackBrainGoal(String abilityId, int cooldownTicks, float halfAngleDeg,
                               double range, float damage, float coeff) {
        this(abilityId, cooldownTicks, 1, halfAngleDeg, range, damage, coeff);
    }

    public ArcAttackBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.targetInMeleeRange() && cooldowns.isReady(abilityId);
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
        if (effects != null && effects.onAction() != null) effects.onAction().accept(entity, world);
        if (effects != null && effects.onTick() != null)   effects.onTick().accept(entity, world);

        var target = entity.getTarget();
        if (target == null || !RangePredicates.isValidTarget(target)) {
            done = true;
            return;
        }

        entity.tryAttack(target);

        DamageUtil.performArcDamage(
                entity,
                target,
                halfAngleDeg,
                range,
                damage + scalingFactor * (float) entity.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE),
                world.getDamageSources().mobAttack(entity),
                world
        );

        if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);
        cooldowns.trigger(abilityId, scaledCooldown(entity));
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
        if (!done && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
    }

    @Override
    public int expectedDurationTicks() {
        return 1;
    }

    @Override
    public java.util.Set<String> declaredAbilityIds() {
        return java.util.Set.of(abilityId);
    }

    protected int scaledCooldown(E entity) {
        return cooldownTicks;
    }
}
