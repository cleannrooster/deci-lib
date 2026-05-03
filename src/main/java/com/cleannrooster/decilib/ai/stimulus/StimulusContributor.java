package com.cleannrooster.decilib.ai.stimulus;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

@FunctionalInterface
public interface StimulusContributor<E extends LivingEntity> {

    void contribute(BaseAIStimulusBuilder builder, E entity, ServerWorld world);
}
