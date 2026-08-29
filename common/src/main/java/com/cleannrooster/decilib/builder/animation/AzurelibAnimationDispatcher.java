package com.cleannrooster.decilib.builder.animation;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;


public final class AzurelibAnimationDispatcher implements MobAnimationDispatcher {

    @Override
    public void trigger(DataDrivenMob entity, String animationName) {
        AzCommand.create("movement_loop", animationName, AzPlayBehaviors.PLAY_ONCE)
                 .sendForEntity(entity);
        // One-shots share the controller with the movement loop, so hold the
        // loop back until the one-shot has finished playing.
        suppressLoopDuring(entity, animationName);
    }

    @Override
    public void triggerLoop(DataDrivenMob entity, String animationName) {
        AzCommand.create("movement_loop", animationName, AzPlayBehaviors.LOOP)
                 .sendForEntity(entity);
    }

    private static void suppressLoopDuring(DataDrivenMob entity, String animationName) {
        var rc = entity.getDefinition().renderConfig();
        if (rc == null) return;
        int ticks = AnimationLengthCache.lengthTicks(rc.animation(), animationName);
        entity.getLoopAnimTracker().suppressFor(ticks);
    }
}
