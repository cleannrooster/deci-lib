package com.cleannrooster.decilib.spawn;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;


public record SpawnEntryConfig(
        int weight,
        int minGroupSize,
        int maxGroupSize,
        List<String> biomes,
        @Nullable String dimension,
        int lightMin,
        int lightMax,
        int heightMin,
        int heightMax,
        Set<SpawnCondition> conditions
) {}
