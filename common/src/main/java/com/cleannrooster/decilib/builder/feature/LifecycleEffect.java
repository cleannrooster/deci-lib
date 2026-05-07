package com.cleannrooster.decilib.builder.feature;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface LifecycleEffect {
    void fire(LivingEntity entity, @Nullable ServerWorld world);
}
