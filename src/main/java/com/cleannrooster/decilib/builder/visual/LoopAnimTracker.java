package com.cleannrooster.decilib.builder.visual;

import com.cleannrooster.decilib.builder.animation.MobAnimationDispatcherRegistry;
import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import org.jetbrains.annotations.Nullable;


public final class LoopAnimTracker {

    @Nullable private String currentAnim  = null;
    private           int    suppressTicks = 0;


    public void suppressFor(int ticks) {
        suppressTicks = Math.max(suppressTicks, ticks);
    }

    public void tick(DataDrivenMob entity) {
        if (suppressTicks > 0) {
            suppressTicks--;
            // When suppression expires, reset currentAnim so the loop
            // resumes unconditionally on the very next tick.
            if (suppressTicks == 0) currentAnim = null;
            return;
        }

        var rc = entity.getDefinition().renderConfig();
        if (rc == null) return;

        var loops = rc.loops();
        if (loops == null) return;

        var hostile = entity.getTarget() != null;
        var state = LoopAnimStateDetector.detect(entity, loops.aiming() != null);
        var anim = loops.resolve(state, hostile);

        if (!anim.equals(currentAnim)) {
            currentAnim = anim;
            MobAnimationDispatcherRegistry.get().triggerLoop(entity, anim);
        }
    }

    public void reset() {
        currentAnim   = null;
        suppressTicks = 0;
    }
}
