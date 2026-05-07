package com.cleannrooster.decilib.ai.util;

public final class CooldownScaler {

    private CooldownScaler() {}

    public static int scale(int baseTicks, int minTicks, float coefficient) {
        int scaled = (int) (baseTicks - baseTicks * coefficient);
        return Math.max(minTicks, scaled);
    }


    public static int scale(int baseTicks, float coefficient) {
        return scale(baseTicks, 1, coefficient);
    }
}
