package com.cleannrooster.decilib.ai.brain;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.statemachine.MobStateMachine;
import com.cleannrooster.decilib.ai.statemachine.Phase;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

public interface MobBrain<T extends LivingEntity> {

    void initialize(T entity, ServerWorld world);
    void tick(T entity, ServerWorld world);
    AIStimulus getCurrentStimulus();
    AbilityCooldownRegistry getCooldowns();
    MobStateMachine<?, ?> getStateMachine();
    Phase getCurrentPhase();
    @Nullable MobBrainGoal<?> getActiveGoal();
    void teardown(T entity, ServerWorld world);
}
