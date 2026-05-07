package com.cleannrooster.decilib.client.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record AnimationProfile(
        float walkAmplitude,
        float idleAmplitude,
        float idleSpeed,
        float attackAmplitude,
        float bodyBob,
        float headTrackStrength
) {
    public static AnimationProfile light() {
        return new AnimationProfile(1.00f, 0.06f, 0.05f, 1.2f, 0.03f, 1.0f);
    }

    public static AnimationProfile heavy() {
        return new AnimationProfile(0.55f, 0.01f, 0.03f, 0.7f, 0.015f, 0.7f);
    }

    public static AnimationProfile agile() {
        return new AnimationProfile(1.30f, 0.10f, 0.07f, 1.8f, 0.05f, 1.0f);
    }

    public static AnimationProfile erratic() {
        return new AnimationProfile(1.10f, 0.20f, 0.13f, 1.5f, 0.04f, 0.6f);
    }

    public static AnimationProfile of(AnimationStyle style) {
        return switch (style) {
            case LIGHT   -> light();
            case HEAVY   -> heavy();
            case AGILE   -> agile();
            case ERRATIC -> erratic();
        };
    }

}
