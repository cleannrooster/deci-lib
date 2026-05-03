package com.cleannrooster.decilib.builder.tuning;

public final class TuningResolver {

    private TuningResolver() {}

    public static double maxHealth(TuningBand band) {
        return switch (band) {
            case NONE   -> 20.0;
            case LOW    -> 40.0;
            case MEDIUM -> 80.0;
            case HIGH   -> 150.0;
        };
    }

    public static double attackDamage(TuningBand band) {
        return switch (band) {
            case NONE   -> 2.0;
            case LOW    -> 4.0;
            case MEDIUM -> 8.0;
            case HIGH   -> 14.0;
        };
    }

    public static double movementSpeed(TuningBand band) {
        return switch (band) {
            case NONE   -> 0.18;
            case LOW    -> 0.22;
            case MEDIUM -> 0.28;
            case HIGH   -> 0.35;
        };
    }

    public static double followRange(TuningBand band) {
        return switch (band) {
            case NONE   -> 8.0;
            case LOW    -> 12.0;
            case MEDIUM -> 16.0;
            case HIGH   -> 24.0;
        };
    }

    public static double knockbackResistance(TuningBand band) {
        return switch (band) {
            case NONE   -> 0.0;
            case LOW    -> 0.2;
            case MEDIUM -> 0.4;
            case HIGH   -> 0.7;
        };
    }
}
