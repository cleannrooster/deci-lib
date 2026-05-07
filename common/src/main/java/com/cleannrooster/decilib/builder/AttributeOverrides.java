package com.cleannrooster.decilib.builder;

import org.jetbrains.annotations.Nullable;


public record AttributeOverrides(
        @Nullable Double maxHealth,
        @Nullable Double attackDamage,
        @Nullable Double movementSpeed,
        @Nullable Double followRange,
        @Nullable Double knockbackResistance,
        @Nullable Double meleeRange
) {}
