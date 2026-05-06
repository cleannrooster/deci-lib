package com.cleannrooster.decilib.ai.goal.common;

import com.cleannrooster.decilib.ai.CanBrace;
import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
// CanBrace used as type bound only — stimulus fields used for condition checks
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class BraceBrainGoal<E extends MobEntity & CanBrace> implements MobBrainGoal<E> {

    private final String  abilityId;
    private final int     cooldownTicks;
    private final int     durationTicks;
    private final float   damageReduction;

    // condition flags
    private final boolean checkHealthThreshold;
    private final float   healthThreshold;
    private final boolean checkRangedHit;
    private final int     rangedHitWindow;
    private final double  rangedHitMinDistance;
    private final boolean checkBurstDamage;
    private final float   burstDamageThreshold;
    private final boolean requireAll;

    @Nullable private GoalEffects<E> effects;

    private int     ticksElapsed;
    private boolean completed;

    public BraceBrainGoal(String abilityId, int cooldownTicks, int durationTicks, float damageReduction,
                           boolean checkHealthThreshold, float healthThreshold,
                           boolean checkRangedHit, int rangedHitWindow, double rangedHitMinDistance,
                           boolean checkBurstDamage, float burstDamageThreshold,
                           boolean requireAll) {
        this.abilityId            = abilityId;
        this.cooldownTicks        = cooldownTicks;
        this.durationTicks        = durationTicks;
        this.damageReduction      = damageReduction;
        this.checkHealthThreshold = checkHealthThreshold;
        this.healthThreshold      = healthThreshold;
        this.checkRangedHit       = checkRangedHit;
        this.rangedHitWindow      = rangedHitWindow;
        this.rangedHitMinDistance = rangedHitMinDistance;
        this.checkBurstDamage     = checkBurstDamage;
        this.burstDamageThreshold = burstDamageThreshold;
        this.requireAll           = requireAll;
    }

    public BraceBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    // ── MobBrainGoal ─────────────────────────────────────────────────────────

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        if (!cooldowns.isReady(abilityId)) return false;
        return conditionsMet(stimulus);
    }

    @Override
    public void reset(E entity) {
        ticksElapsed = 0;
        completed    = false;
    }

    @Override
    public void start(E entity, ServerWorld world) {
        reset(entity);
        entity.enterBraceState(damageReduction);
        entity.getNavigation().stop();
        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        ticksElapsed++;
        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);

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
        entity.exitBraceState();
        if (!completed && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason) {
        entity.exitBraceState();
        if (reason != StopReason.SHUTDOWN && !completed
                && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
    }

    @Override
    public int expectedDurationTicks() { return durationTicks; }

    @Override
    public Set<String> declaredAbilityIds() { return Set.of(abilityId); }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean conditionsMet(AIStimulus stimulus) {
        if (!checkHealthThreshold && !checkRangedHit && !checkBurstDamage) return false;

        if (requireAll) {
            if (checkHealthThreshold && stimulus.selfHealthPct() >= healthThreshold)                          return false;
            if (checkRangedHit       && !rangedHitCondition(stimulus))                                        return false;
            if (checkBurstDamage     && stimulus.recentDamageTaken() < burstDamageThreshold)                  return false;
            return true;
        } else {
            if (checkHealthThreshold && stimulus.selfHealthPct() < healthThreshold)                           return true;
            if (checkRangedHit       && rangedHitCondition(stimulus))                                         return true;
            if (checkBurstDamage     && stimulus.recentDamageTaken() >= burstDamageThreshold)                 return true;
            return false;
        }
    }

    private boolean rangedHitCondition(AIStimulus stimulus) {
        return stimulus.lastHitWasProjectile()
                && stimulus.ticksSinceLastHit() < rangedHitWindow
                && stimulus.targetDistance() > rangedHitMinDistance;
    }
}
