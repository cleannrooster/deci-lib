package com.cleannrooster.decilib.builder.animation;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;


public final class AzurelibAnimationDispatcher implements MobAnimationDispatcher {

    @Override
    public void trigger(DataDrivenMob entity, String animationName) {
        AzCommand.create("movement", animationName, AzPlayBehaviors.PLAY_ONCE)
                 .sendForEntity(entity);
    }

    @Override
    public void triggerLoop(DataDrivenMob entity, String animationName) {
        AzCommand.create("movement_loop", animationName, AzPlayBehaviors.LOOP)
                 .sendForEntity(entity);
    }
}
