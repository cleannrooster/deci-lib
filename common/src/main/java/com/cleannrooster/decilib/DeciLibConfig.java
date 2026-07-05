package com.cleannrooster.decilib;

import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.ConfigData;

@Config(name = "decilib")
public class DeciLibConfig implements ConfigData {

    /**
     * Enables verbose per-tick logging for brain decisions, goal selection,
     * state transitions, and the circuit breaker.
     *
     * Can also be forced on via the JVM flag {@code -Ddecilib.debug=true}
     * regardless of this setting.
     */
    public boolean debugMode = false;

    /**
     * Damage multiplier applied when a data-driven mob deals damage to another mob that
     * shares the same faction. Intentional targeting between faction-mates is always
     * suppressed regardless of this value; this coefficient governs incidental damage
     * (e.g., AoE, projectile splash, explosions, or accidental hits).
     *
     * <p>0.0 = complete immunity to friendly fire; 1.0 = full damage passes through (default).
     * Values between 0 and 1 reduce the damage proportionally.
     */
    public float friendlyFireCoefficient = 1.0f;

    /**
     * If true, mob entity types defined in the built-in {@code data/decilib/mobs_example/}
     * folder are registered on startup. Disable to exclude all example mobs entirely
     * (they will not exist in the game at all).
     *
     * <p>Requires a restart to take effect.
     */
    public boolean registerExampleMobs = true;

    /**
     * If true, example mobs (those loaded from {@code mobs_example/}) are allowed to spawn
     * naturally in the world according to their spawn configs.
     * If false, example mobs can still be summoned manually but will never appear naturally.
     *
     * <p>Has no effect when {@link #registerExampleMobs} is false.
     * Takes effect on the next datapack reload or world join.
     */
    public boolean exampleMobsNaturalSpawning = true;
}
