package com.cleannrooster.decilib.builder.sound;

import org.jetbrains.annotations.Nullable;

/**
 * Entity-level sounds for a data-driven mob, parsed from the root {@code "sounds"}
 * block in the mob's JSON profile.
 *
 * <h3>JSON</h3>
 * <pre>{@code
 * "sounds": {
 *   "idle":   "deci-lib:mob.rogue.idle",
 *   "hurt":   "deci-lib:mob.rogue.hurt",
 *   "death":  "deci-lib:mob.rogue.death",
 *   "step":   "deci-lib:mob.rogue.step",
 *   "attack": { "id": "deci-lib:mob.rogue.swing", "volume": 0.9, "pitch": 1.1 }
 * }
 * }</pre>
 *
 * <p>All fields are optional. Omitting a field falls back to {@code HostileEntity} defaults.
 * Each value is either a bare sound ID string or a {@code {"id","volume","pitch"}} object.
 * Volume and pitch default to {@code 1.0} when the bare string form is used.
 *
 * <p>Sound IDs must be declared in the mod's {@code sounds.json} — nothing is auto-registered.
 */
public record SoundConfig(
        @Nullable SoundEntry idle,
        @Nullable SoundEntry hurt,
        @Nullable SoundEntry death,
        @Nullable SoundEntry step,
        @Nullable SoundEntry attack
) {}
