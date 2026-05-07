package com.cleannrooster.decilib.ai.statemachine;

import net.minecraft.entity.LivingEntity;

public interface StanceLifecycleHandler<S extends Enum<S>> {

    void onStanceEnter(S newStance, LivingEntity entity);

    void onStanceExit(S oldStance, LivingEntity entity);
}
