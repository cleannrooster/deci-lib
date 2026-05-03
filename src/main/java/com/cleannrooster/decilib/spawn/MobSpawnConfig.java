package com.cleannrooster.decilib.spawn;

import net.minecraft.entity.SpawnGroup;

import java.util.List;


public record MobSpawnConfig(
        String mobId,
        SpawnGroup spawnGroup,
        List<SpawnEntryConfig> entries
) {}
