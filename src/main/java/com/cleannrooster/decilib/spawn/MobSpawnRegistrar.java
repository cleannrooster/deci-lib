package com.cleannrooster.decilib.spawn;

import com.cleannrooster.decilib.Decilib;
import com.cleannrooster.decilib.entity.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;


public final class MobSpawnRegistrar {

    private MobSpawnRegistrar() {}

    public static void registerAll() {
        for (MobSpawnConfig config : MobSpawnRegistry.all()) {
            register(config);
        }
    }

    @SuppressWarnings("unchecked")
    private static void register(MobSpawnConfig config) {
        EntityType<?> rawType = ModEntities.getDataDrivenMobs().get(config.mobId());
        if (rawType == null) {
            Decilib.LOGGER.warn("[deci-lib] Spawn config for '{}': entity type not registered, skipping",
                    config.mobId());
            return;
        }

        EntityType<? extends MobEntity> type = (EntityType<? extends MobEntity>) rawType;

        // Add to biome spawn lists — one addSpawn call per entry
        for (SpawnEntryConfig entry : config.entries()) {
            var spawnEntry = new SpawnSettings.SpawnEntry(
                    type, entry.weight(), entry.minGroupSize(), entry.maxGroupSize());
            BiomeModifications.addSpawn(buildBiomeSelector(entry), config.spawnGroup(), spawnEntry.type, entry.weight(),entry.minGroupSize(), entry.maxGroupSize() );
        }

        // Runtime predicate — checked per spawn attempt, ANY entry match = allow
        SpawnRestriction.register(
                type,
                SpawnLocationTypes.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (entityType, world, reason, pos, random) -> anyEntryAllows(config.entries(), world, reason, pos));
    }

    // -------------------------------------------------------------------------
    // Biome selector (runs at biome loading / registration time)
    // -------------------------------------------------------------------------

    private static Predicate<BiomeSelectionContext> buildBiomeSelector(SpawnEntryConfig entry) {
        Predicate<BiomeSelectionContext> selector = entry.biomes().isEmpty()
                ? ctx -> true
                : buildBiomesPredicate(entry.biomes());

        if (entry.dimension() != null) {
            selector = selector.and(buildDimensionPredicate(entry.dimension()));
        }
        return selector;
    }

    private static Predicate<BiomeSelectionContext> buildBiomesPredicate(List<String> biomes) {
        var preds = new ArrayList<Predicate<BiomeSelectionContext>>();
        for (String spec : biomes) {
            if (spec.startsWith("#")) {
                TagKey<Biome> tag = TagKey.of(RegistryKeys.BIOME, Identifier.of(spec.substring(1)));
                preds.add(ctx -> ctx.hasTag(tag));
            } else {
                Identifier id = Identifier.of(spec);
                preds.add(ctx -> ctx.getBiomeKey().getValue().equals(id));
            }
        }
        return preds.stream().reduce(ctx -> false, Predicate::or);
    }

    private static Predicate<BiomeSelectionContext> buildDimensionPredicate(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld"  -> ctx -> ctx.hasTag(
                    TagKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "is_overworld")));
            case "minecraft:the_nether" -> ctx -> ctx.hasTag(
                    TagKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "is_nether")));
            case "minecraft:the_end"    -> ctx -> ctx.hasTag(
                    TagKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "is_end")));
            default -> {
                Decilib.LOGGER.warn("[deci-lib] Unknown dimension '{}' in spawn config, ignoring dimension filter",
                        dimension);
                yield ctx -> true;
            }
        };
    }

    // -------------------------------------------------------------------------
    // Runtime spawn predicate (runs per natural spawn attempt)
    // -------------------------------------------------------------------------

    private static boolean anyEntryAllows(List<SpawnEntryConfig> entries,
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
        // Dimension — needs a concrete ServerWorld
        if (entry.dimension() != null && world instanceof net.minecraft.server.world.ServerWorld sw) {
            if (!sw.getRegistryKey().getValue().toString().equals(entry.dimension())) return false;
        }

        // Height
        var y = pos.getY();
        if (y < entry.heightMin() || y > entry.heightMax()) return false;

        // Block light level
        var light = world.getLightLevel(LightType.BLOCK, pos);
        if (light < entry.lightMin() || light > entry.lightMax()) return false;

        // Conditions
        for (SpawnCondition cond : entry.conditions()) {
            switch (cond) {
                case ON_GROUND    -> { if (!world.getBlockState(pos.down()).isSolid()) return false; }
                case NOT_IN_WATER -> { if (!world.getFluidState(pos).isEmpty()) return false; }
                case IN_WATER     -> { if (world.getFluidState(pos).isEmpty()) return false; }
                case NATURAL_ONLY -> { if (reason != SpawnReason.NATURAL) return false; }
            }
        }

        // Biome — exact runtime check (double-checks BiomeModifications approximation)
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
