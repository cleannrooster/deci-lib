package com.cleannrooster.decilib.spawn;

import com.cleannrooster.decilib.DeciLibConfig;
import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.entity.ModEntities;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.biome.Biome;

import java.util.List;


public final class MobSpawnRegistrar {

    private MobSpawnRegistrar() {}

    /**
     * Registers a SpawnRestriction for every data-driven entity type via the vanilla
     * SpawnRestriction.register() call. Works on Fabric (class loader does not enforce
     * JPMS private access). Do NOT call this on NeoForge — use
     * {@link #buildSpawnPredicate(String)} with RegisterSpawnPlacementsEvent instead.
     */
    public static void registerSpawnRestrictions() {
        for (var entry : ModEntities.getDataDrivenMobs().entrySet()) {
            String mobId = entry.getKey();
            EntityType<DataDrivenMob> type = entry.getValue();
            SpawnRestriction.register(
                    type,
                    SpawnLocationTypes.ON_GROUND,
                    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    buildSpawnPredicate(mobId));
        }
    }

    /**
     * Returns the spawn predicate for a given mob ID. Used by NeoForge's
     * RegisterSpawnPlacementsEvent to register spawn restrictions without
     * calling the private SpawnRestriction.register() directly.
     */
    public static SpawnRestriction.SpawnPredicate<DataDrivenMob> buildSpawnPredicate(String mobId) {
        return (entityType, world, reason, pos, random) -> {
            // Example mobs respect the exampleMobsNaturalSpawning config flag.
            if (ModEntities.isExampleMob(mobId)) {
                var cfg = AutoConfig.getConfigHolder(DeciLibConfig.class).getConfig();
                if (!cfg.exampleMobsNaturalSpawning) return false;
            }
            return MobSpawnRegistry.get(mobId)
                    .map(c -> anyEntryAllows(c.entries(), world, reason, pos))
                    .orElse(false);
        };
    }

    /**
     * Returns true if the given biome registry entry matches the biome/dimension constraints
     * of a spawn entry. Used by both platforms at biome-build time:
     * <ul>
     *   <li>Fabric: BiomeModifications lambda in DecilibFabric via ctx.getBiome()</li>
     *   <li>NeoForge: DynamicMobSpawnModifier via its BiomeModifier RegistryEntry&lt;Biome&gt;</li>
     * </ul>
     * Both platforms supply a vanilla {@code RegistryEntry<Biome>}, so isIn/matchesId work
     * identically on both.
     */
    public static boolean biomeMatchesEntry(RegistryEntry<Biome> biomeEntry, SpawnEntryConfig entry) {
        if (entry.biomes().isEmpty()) return true;
        for (String spec : entry.biomes()) {
            if (spec.startsWith("#")) {
                TagKey<Biome> tag = TagKey.of(RegistryKeys.BIOME, Identifier.of(spec.substring(1)));
                if (biomeEntry.isIn(tag)) return true;
            } else {
                if (biomeEntry.matchesId(Identifier.of(spec))) return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Runtime spawn predicate (runs per natural spawn attempt — both platforms)
    // -------------------------------------------------------------------------

    static boolean anyEntryAllows(List<SpawnEntryConfig> entries,
                                  ServerWorldAccess world,
                                  SpawnReason reason,
                                  BlockPos pos) {
        for (SpawnEntryConfig entry : entries) {
            if (entryAllows(entry, world, reason, pos)) return true;
        }
        return false;
    }

    private static boolean entryAllows(SpawnEntryConfig entry,
                                       ServerWorldAccess world,
                                       SpawnReason reason,
                                       BlockPos pos) {
        if (entry.dimension() != null && world instanceof net.minecraft.server.world.ServerWorld sw) {
            if (!sw.getRegistryKey().getValue().toString().equals(entry.dimension())) return false;
        }

        var y = pos.getY();
        if (y < entry.heightMin() || y > entry.heightMax()) return false;

        var light = world.getLightLevel(LightType.BLOCK, pos);
        if (light < entry.lightMin() || light > entry.lightMax()) return false;

        for (SpawnCondition cond : entry.conditions()) {
            switch (cond) {
                case ON_GROUND    -> { if (!world.getBlockState(pos.down()).isSolid()) return false; }
                case NOT_IN_WATER -> { if (!world.getFluidState(pos).isEmpty()) return false; }
                case IN_WATER     -> { if (world.getFluidState(pos).isEmpty()) return false; }
                case NATURAL_ONLY -> { if (reason != SpawnReason.NATURAL) return false; }
            }
        }

        if (!entry.biomes().isEmpty()) {
            var biomeEntry = world.getBiome(pos);
            boolean biomeMatch = false;
            for (String spec : entry.biomes()) {
                if (spec.startsWith("#")) {
                    TagKey<Biome> tag = TagKey.of(RegistryKeys.BIOME, Identifier.of(spec.substring(1)));
                    if (biomeEntry.isIn(tag)) { biomeMatch = true; break; }
                } else {
                    if (biomeEntry.matchesId(Identifier.of(spec))) { biomeMatch = true; break; }
                }
            }
            if (!biomeMatch) return false;
        }

        return true;
    }
}
