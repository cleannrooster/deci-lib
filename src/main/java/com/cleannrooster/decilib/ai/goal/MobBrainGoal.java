package com.cleannrooster.decilib.ai.goal;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Set;

public interface MobBrainGoal<E extends LivingEntity> {

    boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns);

    void start(E entity, ServerWorld world);

    void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns);

    boolean shouldContinue(E entity, AIStimulus stimulus);

    void stop(E entity, ServerWorld world);

    default int expectedDurationTicks() {
        return -1;
    }

    default boolean isCommitted() {
        return false;
    }

    default void reset(E entity) {}

    default Set<String> declaredAbilityIds() {
        return Set.of();
    }

    default void stop(E entity, ServerWorld world, StopReason reason) {
        stop(entity, world);
    }

    default void stop(E entity, ServerWorld world, StopReason reason, AbilityCooldownRegistry cooldowns) {
        stop(entity, world, reason);
    }
}
