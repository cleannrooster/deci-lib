package com.cleannrooster.decilib.builder.visual;

import org.jetbrains.annotations.Nullable;


public record LoopAnimSet(
        String           idle,
        @Nullable String idleHostile,
        @Nullable String moving,
        @Nullable String movingHostile,
        @Nullable String running,
        @Nullable String aiming
) {

    public String resolve(LoopAnimState state, boolean hostile) {
        return switch (state) {
            case AIMING  -> aiming != null ? aiming : resolveIdleWithFallback(hostile);
            case RUNNING -> running != null ? running : resolve(LoopAnimState.MOVING, hostile);
            case MOVING  -> {
                if (moving == null) yield resolveIdleWithFallback(hostile);
                yield hostile && movingHostile != null ? movingHostile : moving;
            }
            case IDLE    -> resolveIdleWithFallback(hostile);
        };
    }

    private String resolveIdleWithFallback(boolean hostile) {
        return hostile && idleHostile != null ? idleHostile : idle;
    }
}
