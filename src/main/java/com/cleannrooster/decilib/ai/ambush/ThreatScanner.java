package com.cleannrooster.decilib.ai.ambush;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ThreatScanner<E extends MobEntity> {

    /**
     * Attempts to find a threat within {@code range} blocks of {@code entity}.
     */
    @Nullable LivingEntity scan(E entity, ServerWorld world, double range);
}
