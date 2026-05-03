package com.cleannrooster.decilib.ai.goal.common;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import com.cleannrooster.decilib.ai.util.DamageUtil;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class RadialAttackBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    private final String abilityId;
    private final int    cooldownTicks;
    private final int    minCooldownTicks;
    private final double range;
    private final float  damage;
    private final float  scalingFactor;

    @Nullable private GoalEffects<E> effects;

    private boolean done;

    public RadialAttackBrainGoal(String abilityId, int cooldownTicks, int minCooldownTicks,
                                  double range, float damage, float coeff) {
        this.abilityId        = abilityId;
        this.cooldownTicks    = cooldownTicks;
        this.minCooldownTicks = minCooldownTicks;
        this.range            = range;
        this.damage           = damage;
        this.scalingFactor    = coeff;
    }

    public RadialAttackBrainGoal(String abilityId, int cooldownTicks, double range, float damage, float coeff) {
        this(abilityId, cooldownTicks, 1, range, damage, coeff);
    }

    public RadialAttackBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.hasTarget() && cooldowns.isReady(abilityId);
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

        DamageUtil.performRadialDamage(
                entity,
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
    public Set<String> declaredAbilityIds() {
        return Set.of(abilityId);
    }

    protected int scaledCooldown(E entity) {
        return cooldownTicks;
    }
}
