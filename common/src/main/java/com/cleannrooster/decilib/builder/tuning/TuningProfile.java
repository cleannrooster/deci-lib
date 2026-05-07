package com.cleannrooster.decilib.builder.tuning;


public record TuningProfile(
        TuningBand health,
        TuningBand damage,
        TuningBand speed,
        TuningBand detection
) {
    public static TuningProfile defaults() {
        return new TuningProfile(TuningBand.MEDIUM, TuningBand.MEDIUM, TuningBand.MEDIUM, TuningBand.MEDIUM);
    }
}
