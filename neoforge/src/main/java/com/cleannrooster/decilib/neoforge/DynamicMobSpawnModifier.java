package com.cleannrooster.decilib.neoforge;

import com.cleannrooster.decilib.Decilib;
import com.cleannrooster.decilib.DeciLibConfig;
import com.cleannrooster.decilib.entity.ModEntities;
import com.cleannrooster.decilib.spawn.MobSpawnConfig;
import com.cleannrooster.decilib.spawn.MobSpawnRegistrar;
import com.cleannrooster.decilib.spawn.MobSpawnRegistry;
import com.cleannrooster.decilib.spawn.SpawnEntryConfig;
import me.shedaniel.autoconfig.AutoConfig;
import com.mojang.serialization.MapCodec;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * NeoForge BiomeModifier that lazily reads from MobSpawnRegistry at biome-build time.
 * Registered as a codec via RegisterEvent (BIOME_MODIFIER_SERIALIZERS) in DecilibNeoForge.
 * Activated by the JSON at: data/deci-lib/neoforge/biome_modifier/mob_spawns.json
 */
public record DynamicMobSpawnModifier() implements BiomeModifier {

    /** Unit codec — this modifier has no configuration; all data comes from MobSpawnRegistry. */
    public static final MapCodec<DynamicMobSpawnModifier> CODEC =
            MapCodec.unit(new DynamicMobSpawnModifier());

    @Override
    public void modify(RegistryEntry<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD) return;

        var cfg = AutoConfig.getConfigHolder(DeciLibConfig.class).getConfig();
        for (MobSpawnConfig config : MobSpawnRegistry.all()) {
            // Skip example mobs from biome spawn lists when natural spawning is off.
            if (ModEntities.isExampleMob(config.mobId()) && !cfg.exampleMobsNaturalSpawning) {
                continue;
            }
            EntityType<?> rawType = ModEntities.getDataDrivenMobs().get(config.mobId());
            if (rawType == null) {
                Decilib.LOGGER.warn("[deci-lib] BiomeModifier: entity '{}' not registered, skipping",
                        config.mobId());
                continue;
            }

            @SuppressWarnings("unchecked")
            EntityType<? extends MobEntity> type = (EntityType<? extends MobEntity>) rawType;

            for (SpawnEntryConfig entry : config.entries()) {
                if (!MobSpawnRegistrar.biomeMatchesEntry(biome, entry)) continue;
                // Dimension filtering is done at runtime spawn time via entryAllows(),
                // not at biome-build time on NeoForge (no registry key on RegistryEntry<Biome>).
                builder.getMobSpawnSettings().spawn(
                        config.spawnGroup(),
                        new SpawnSettings.SpawnEntry(
                                type, entry.weight(), entry.minGroupSize(), entry.maxGroupSize()));
            }
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
