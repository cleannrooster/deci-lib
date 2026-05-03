package com.cleannrooster.decilib.ai.ambush;

import net.minecraft.entity.mob.MobEntity;

import java.util.Objects;

public record AmbushConfig<E extends MobEntity & CanAmbush>(
        double detectionRange,
        double ambushRadius,
        int    scanIntervalTicks,
        int    rehideDelayTicks,
        ThreatScanner<E> scanner
) {
    public static <E extends MobEntity & CanAmbush> Builder<E> builder() {
        return new Builder<>();
    }

    public static final class Builder<E extends MobEntity & CanAmbush> {

        private double           detectionRange   = 16.0;
        private double           ambushRadius     = 6.0;
        private int              scanIntervalTicks = 10;
        private int              rehideDelayTicks  = 60;
        private ThreatScanner<E> scanner;

        /** Radius within which the scanner looks for threats (default 16). */
        public Builder<E> detectionRange(double v)   { this.detectionRange    = v; return this; }

        /**
         * Threshold below which a detected threat causes the entity to surface
         * (default 6). Must be ≤ detectionRange
         */
        public Builder<E> ambushRadius(double v)     { this.ambushRadius      = v; return this; }

        /** Ticks between scan invocations while in the hidden watch phase (default 10). */
        public Builder<E> scanInterval(int v)        { this.scanIntervalTicks = v; return this; }

        /** Ticks to wait after losing a target before re-entering hidden state (default 60). */
        public Builder<E> rehideDelay(int v)         { this.rehideDelayTicks  = v; return this; }

        /** Strategy that locates a nearby threat. Required. */
        public Builder<E> scanner(ThreatScanner<E> v) { this.scanner          = v; return this; }

        public AmbushConfig<E> build() {
            Objects.requireNonNull(scanner, "AmbushConfig: scanner must be set");
            return new AmbushConfig<>(detectionRange, ambushRadius, scanIntervalTicks,
                    rehideDelayTicks, scanner);
        }
    }
}
