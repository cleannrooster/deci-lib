package com.cleannrooster.decilib.builder.visual;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;

public final class LoopAnimStateDetector {

    public static final double MOVE_THRESHOLD = 0.03;
    public static final double RUN_THRESHOLD  = 0.18;

    private LoopAnimStateDetector() {}

    public static LoopAnimState detect(DataDrivenMob entity, boolean hasAimingAnim) {
        var hSpeed = horizontalSpeed(entity);
        var hostile = entity.getTarget() != null;

        // AIMING: hostile, nearly stationary, aiming animation available
        if (hostile && hasAimingAnim && hSpeed < MOVE_THRESHOLD) {
            return LoopAnimState.AIMING;
        }
        if (hSpeed >= RUN_THRESHOLD)  return LoopAnimState.RUNNING;
        if (hSpeed >= MOVE_THRESHOLD) return LoopAnimState.MOVING;
        return LoopAnimState.IDLE;
    }

    private static double horizontalSpeed(DataDrivenMob entity) {
        var vel = entity.getVelocity();
        return Math.sqrt(vel.x * vel.x + vel.z * vel.z);
    }
}
