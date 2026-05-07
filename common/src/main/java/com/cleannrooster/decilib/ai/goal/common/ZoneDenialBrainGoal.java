package com.cleannrooster.decilib.ai.goal.common;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import com.cleannrooster.decilib.ai.util.RangePredicates;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ZoneDenialBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    private static final class Hazard {
        final Vec3d pos;
        int remaining;
        Hazard(Vec3d pos, int lifetime) { this.pos = pos; this.remaining = lifetime; }
        boolean tick() { return --remaining <= 0; }
    }

    private final String abilityId;
    private final int    cooldownTicks;
    private final int    goalDurationTicks;
    private final int    hazardLifetimeTicks;
    private final double hazardRadius;
    private final float  hazardDamagePerTick;
    private final int    placeIntervalTicks;
    private final int    maxHazards;
    private final double minPlaceDistance;
    private final double approachSpeed;

    @Nullable private GoalEffects<E> effects;

    private final List<Hazard> hazards = new ArrayList<>();
    private int     ticksElapsed;
    private boolean completed;

    public ZoneDenialBrainGoal(String abilityId, int cooldownTicks, int goalDurationTicks,
                                int hazardLifetimeTicks, double hazardRadius, float hazardDamagePerTick,
                                int placeIntervalTicks, int maxHazards, double minPlaceDistance,
                                double approachSpeed) {
        this.abilityId           = abilityId;
        this.cooldownTicks       = cooldownTicks;
        this.goalDurationTicks   = goalDurationTicks;
        this.hazardLifetimeTicks = hazardLifetimeTicks;
        this.hazardRadius        = hazardRadius;
        this.hazardDamagePerTick = hazardDamagePerTick;
        this.placeIntervalTicks  = placeIntervalTicks;
        this.maxHazards          = maxHazards;
        this.minPlaceDistance    = minPlaceDistance;
        this.approachSpeed       = approachSpeed;
    }

    public ZoneDenialBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.hasTarget()
                && stimulus.targetDistance() > minPlaceDistance
                && cooldowns.isReady(abilityId);
    }

    @Override
    public void reset(E entity) {
        ticksElapsed = 0;
        completed    = false;
        hazards.clear();
    }

    @Override
    public void start(E entity, ServerWorld world) {
        reset(entity);
        placeHazardAtTarget(entity);
        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        ticksElapsed++;
        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);

        var target = entity.getTarget();
        if (target != null) {
            entity.getLookControl().lookAt(target, 30f, 30f);
            entity.getNavigation().startMovingTo(target, approachSpeed);
        }

        if (ticksElapsed % placeIntervalTicks == 0 && target != null && hazards.size() < maxHazards) {
            placeHazardAtTarget(entity);
        }

        var source = world.getDamageSources().mobAttack(entity);
        tickAndApplyHazards(world, entity, source);

        if (ticksElapsed >= goalDurationTicks) {
            if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);
            cooldowns.trigger(abilityId, cooldownTicks);
            completed = true;
        }
    }

    @Override
    public boolean shouldContinue(E entity, AIStimulus stimulus) {
        return !completed && stimulus.hasTarget();
    }

    @Override
    public void stop(E entity, ServerWorld world) {
        hazards.clear();
        if (!completed && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason) {
        hazards.clear();
        if (reason != StopReason.SHUTDOWN && !completed
                && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
    }

    @Override
    public int expectedDurationTicks() { return goalDurationTicks; }

    @Override
    public Set<String> declaredAbilityIds() { return Set.of(abilityId); }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void placeHazardAtTarget(E entity) {
        var target = entity.getTarget();
        if (target != null) hazards.add(new Hazard(target.getPos(), hazardLifetimeTicks));
    }

    private void tickAndApplyHazards(ServerWorld world, E entity, DamageSource source) {
        hazards.removeIf(h -> {
            spawnHazardParticles(world, h.pos);
            Box searchBox = Box.of(h.pos, hazardRadius * 2, 2.0, hazardRadius * 2);
            for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, searchBox, e -> true)) {
                if (living == entity) continue;
                if (!RangePredicates.isValidTarget(living)) continue;
                if (entity.isTeammate(living)) continue;
                if (living.squaredDistanceTo(h.pos.x, h.pos.y, h.pos.z) <= hazardRadius * hazardRadius) {
                    living.damage(source, hazardDamagePerTick);
                }
            }
            return h.tick();
        });
    }

    private void spawnHazardParticles(ServerWorld world, Vec3d pos) {
        world.spawnParticles(ParticleTypes.FLAME,
                pos.x, pos.y + 0.1, pos.z,
                2, hazardRadius * 0.3, 0.1, hazardRadius * 0.3, 0.02);
    }
}
