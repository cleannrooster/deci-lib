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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class RangePunishBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    private enum Phase { WINDUP, WAVE, COMPLETE }

    private final String abilityId;
    private final int    cooldownTicks;
    private final int    interruptCooldownTicks;
    private final int    windupTicks;
    private final double safeZoneRadius;
    private final double waveWidth;
    private final double waveSpeed;
    private final double maxRange;
    private final float  damage;
    private final float  coeff;

    @Nullable private GoalEffects<E> effects;

    private Phase  phase;
    private int    windupRemaining;
    private double currentRadius;
    private boolean completed;

    public RangePunishBrainGoal(String abilityId, int cooldownTicks, int interruptCooldownTicks,
                                 int windupTicks, double safeZoneRadius, double waveWidth,
                                 double waveSpeed, double maxRange, float damage, float coeff) {
        this.abilityId              = abilityId;
        this.cooldownTicks          = cooldownTicks;
        this.interruptCooldownTicks = interruptCooldownTicks;
        this.windupTicks            = windupTicks;
        this.safeZoneRadius         = safeZoneRadius;
        this.waveWidth              = waveWidth;
        this.waveSpeed              = waveSpeed;
        this.maxRange               = maxRange;
        this.damage                 = damage;
        this.coeff                  = coeff;
    }

    public RangePunishBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    @Override
    public boolean isCommitted() { return !completed; }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.hasTarget()
                && !stimulus.targetInMeleeRange()
                && cooldowns.isReady(abilityId);
    }

    @Override
    public void reset(E entity) {
        phase           = Phase.WINDUP;
        windupRemaining = windupTicks;
        currentRadius   = safeZoneRadius + waveWidth;
        completed       = false;
    }

    @Override
    public void start(E entity, ServerWorld world) {
        reset(entity);
        entity.getNavigation().stop();
        var target = entity.getTarget();
        if (target != null) entity.lookAtEntity(target, 180f, 180f);
        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        switch (phase) {
            case WINDUP   -> tickWindup(entity, world);
            case WAVE     -> tickWave(entity, world, cooldowns);
            case COMPLETE -> {}
        }
        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);
    }

    private void tickWindup(E entity, ServerWorld world) {
        windupRemaining--;
        // Stomp buildup particles — converging toward the mob
        world.spawnParticles(ParticleTypes.LARGE_SMOKE,
                entity.getX(), entity.getY() + 0.2, entity.getZ(),
                4, 0.5, 0.1, 0.5, 0.02);

        if (windupRemaining <= 0) {
            // Stomp — transition to wave
            world.spawnParticles(ParticleTypes.EXPLOSION,
                    entity.getX(), entity.getY() + 0.1, entity.getZ(),
                    1, 0, 0, 0, 0);
            phase = Phase.WAVE;
        }
    }

    private void tickWave(E entity, ServerWorld world, AbilityCooldownRegistry cooldowns) {
        currentRadius += waveSpeed;
        double inner = currentRadius - waveWidth;
        double outer = currentRadius;

        // Ring damage
        float totalDmg = damage + coeff * (float) entity.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        DamageUtil.performRingDamage(entity, inner, outer, totalDmg,
                world.getDamageSources().mobAttack(entity), world);

        // Ring particles
        spawnRingParticles(world, entity, currentRadius);

        if (currentRadius > maxRange) {
            if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);
            cooldowns.trigger(abilityId, cooldownTicks);
            completed = true;
            phase = Phase.COMPLETE;
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
        completed = true;
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason) {
        if (reason != StopReason.SHUTDOWN && !completed
                && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
        completed = true;
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason, AbilityCooldownRegistry cooldowns) {
        if (reason != StopReason.SHUTDOWN && !completed) {
            if (effects != null && effects.onInterrupt() != null) effects.onInterrupt().accept(entity, world);
            if (phase == Phase.WINDUP) cooldowns.trigger(abilityId, interruptCooldownTicks);
        }
        completed = true;
    }

    @Override
    public int expectedDurationTicks() {
        return windupTicks + (int) Math.ceil((maxRange - safeZoneRadius) / waveSpeed);
    }

    @Override
    public Set<String> declaredAbilityIds() { return Set.of(abilityId); }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void spawnRingParticles(ServerWorld world, E entity, double radius) {
        int count = Math.max(8, (int) (radius * 4));
        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2.0 / count;
            double px = entity.getX() + Math.cos(angle) * radius;
            double pz = entity.getZ() + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.SMOKE, px, entity.getY() + 0.2, pz,
                    1, 0.1, 0.05, 0.1, 0.0);
        }
    }
}
