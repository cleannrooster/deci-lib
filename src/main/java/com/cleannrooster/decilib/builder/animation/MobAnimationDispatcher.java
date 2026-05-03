package com.cleannrooster.decilib.builder.animation;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;

public interface MobAnimationDispatcher {
    void trigger(DataDrivenMob entity, String animationName);

    default void triggerLoop(DataDrivenMob entity, String animationName) {
        trigger(entity, animationName);
    }

    MobAnimationDispatcher NOOP = (entity, name) -> {};
}
