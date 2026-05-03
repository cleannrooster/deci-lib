package com.cleannrooster.decilib.builder.archetype;

public record BaseStats(
        double maxHealth,
        double attackDamage,
        double movementSpeed,
        double followRange,
        double knockbackResistance,
        double meleeRange
) {}
