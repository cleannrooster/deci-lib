package com.cleannrooster.decilib.ai.goal;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Wraps a {@link MobBrainGoal} and adds a stimulus-gate check to {@link #canStart}.
 * The gate is evaluated before delegating to the inner goal — if the gate fails,
 * {@code canStart} returns {@code false} and the goal is never selected.
 *
 * <p>{@link #shouldContinue} is intentionally NOT gated: once a goal has started
 * it is allowed to finish naturally, avoiding jitter when gate conditions fluctuate
 * mid-execution.
 *
 * <p>Created by {@link com.cleannrooster.decilib.builder.feature.BehaviorComposer#addOffensiveGoal}
 * to apply the AI profile's combined offensive gate to goals that lack a dedicated
 * entry transition.
 */
@SuppressWarnings("unchecked")
public final class GatedBrainGoal<E extends LivingEntity> implements MobBrainGoal<E> {

    private final MobBrainGoal<E>       inner;
    private final Predicate<AIStimulus> gate;

    public GatedBrainGoal(MobBrainGoal<?> inner, Predicate<AIStimulus> gate) {
        this.inner = (MobBrainGoal<E>) inner;
        this.gate  = gate;
    }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return gate.test(stimulus) && inner.canStart(stimulus, cooldowns);
    }

    @Override
    public void start(E entity, ServerWorld world) { inner.start(entity, world); }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        inner.tick(entity, world, stimulus, cooldowns);
    }

    @Override
    public boolean shouldContinue(E entity, AIStimulus stimulus) { return inner.shouldContinue(entity, stimulus); }

    @Override
    public void stop(E entity, ServerWorld world) { inner.stop(entity, world); }

    @Override
    public int expectedDurationTicks() { return inner.expectedDurationTicks(); }

    @Override
    public boolean isCommitted() { return inner.isCommitted(); }

    @Override
    public void reset(E entity) { inner.reset(entity); }

    @Override
    public Set<String> declaredAbilityIds() { return inner.declaredAbilityIds(); }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason) {
        inner.stop(entity, world, reason);
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason, AbilityCooldownRegistry cooldowns) {
        inner.stop(entity, world, reason, cooldowns);
    }
}
