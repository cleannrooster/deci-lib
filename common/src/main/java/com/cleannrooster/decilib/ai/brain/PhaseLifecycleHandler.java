package com.cleannrooster.decilib.ai.brain;

import com.cleannrooster.decilib.ai.statemachine.Phase;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

public interface PhaseLifecycleHandler<T extends LivingEntity> {

    void onPhaseEnter(Phase newPhase, T entity, ServerWorld world);

    void onPhaseExit(Phase oldPhase, T entity, ServerWorld world);
}
