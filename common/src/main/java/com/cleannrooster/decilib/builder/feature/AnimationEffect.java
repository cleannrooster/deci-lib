package com.cleannrooster.decilib.builder.feature;

import com.cleannrooster.decilib.builder.animation.MobAnimationDispatcherRegistry;
import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

public record AnimationEffect(String animationName) implements LifecycleEffect {

    @Override
    public void fire(LivingEntity entity, @Nullable ServerWorld world) {
        if (entity instanceof DataDrivenMob mob) {
            MobAnimationDispatcherRegistry.get().trigger(mob, animationName);
        }
    }
}
