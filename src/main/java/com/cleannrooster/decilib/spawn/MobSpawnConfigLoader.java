package com.cleannrooster.decilib.spawn;

import com.cleannrooster.decilib.Decilib;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.SpawnGroup;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;


public final class MobSpawnConfigLoader {

    private static final String SPAWN_PATH = "data/deci-lib/mob_spawns";

    private MobSpawnConfigLoader() {}


    public static void loadAll() {
        FabricLoader.getInstance().getModContainer(Decilib.MOD_ID).ifPresent(container -> {
            for (Path root : container.getRootPaths()) {
                Path spawnDir = root.resolve(SPAWN_PATH);
                if (!Files.isDirectory(spawnDir)) continue;

                try (Stream<Path> paths = Files.walk(spawnDir, 1)) {
                    paths.filter(p -> p.getFileName().toString().endsWith(".json"))
                         .forEach(MobSpawnConfigLoader::loadFile);
                } catch (IOException e) {
                    Decilib.LOGGER.error("[deci-lib] Failed to scan spawn configs in {}: {}",
                            spawnDir, e.getMessage(), e);
                }
            }
        });
    }

    private static void loadFile(Path path) {
        try (Reader r = Files.newBufferedReader(path)) {
            JsonObject json = JsonParser.parseReader(r).getAsJsonObject();
            MobSpawnConfig config = parse(json, path.getFileName().toString());
            MobSpawnRegistry.register(config);
            Decilib.LOGGER.info("[deci-lib] Loaded spawn config '{}' ({} entries)",
                    config.mobId(), config.entries().size());
        } catch (Exception e) {
            Decilib.LOGGER.error("[deci-lib] Failed to load spawn config {}: {}", path.getFileName(), e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------

    static MobSpawnConfig parse(JsonObject json, String source) {
        if (!json.has("mob_id")) {
            throw new IllegalArgumentException("Spawn config '" + source + "' is missing required 'mob_id' field");
        }

        var mobId        = json.get("mob_id").getAsString();
        SpawnGroup group = json.has("category")
                ? parseSpawnGroup(json.get("category").getAsString(), source)
                : SpawnGroup.MONSTER;

        var entries = new ArrayList<SpawnEntryConfig>();
        if (json.has("spawns")) {
            for (JsonElement elem : json.getAsJsonArray("spawns")) {
                entries.add(parseEntry(elem.getAsJsonObject(), source, mobId));
            }
        }

        return new MobSpawnConfig(mobId, group, List.copyOf(entries));
    }

    private static SpawnEntryConfig parseEntry(JsonObject obj, String source, String mobId) {
        int weight   = obj.has("weight") ? obj.get("weight").getAsInt() : 10;
        int minGroup = 1, maxGroup = 1;
        if (obj.has("group_size")) {
            var arr = obj.getAsJsonArray("group_size");
            if (arr.size() != 2) {
                throw new IllegalArgumentException("Spawn config '" + source + "' mob '" + mobId
                        + "': group_size must be a 2-element array [min, max]");
            }
            minGroup = arr.get(0).getAsInt();
            maxGroup = arr.get(1).getAsInt();
            if (minGroup < 1 || maxGroup < minGroup) {
                throw new IllegalArgumentException("Spawn config '" + source + "' mob '" + mobId
                        + "': group_size must satisfy 1 <= min <= max");
            }
        }

        var biomes = new ArrayList<String>();
        if (obj.has("biomes")) {
            var biomesElem = obj.get("biomes");
            if (biomesElem.isJsonArray()) {
                for (JsonElement e : biomesElem.getAsJsonArray()) biomes.add(e.getAsString());
            } else {
                biomes.add(biomesElem.getAsString());
            }
        }

        String dimension = obj.has("dimension") ? obj.get("dimension").getAsString() : null;

        int lightMin = 0, lightMax = 15;
        if (obj.has("light_level")) {
            var light = obj.getAsJsonObject("light_level");
            lightMin = light.has("min") ? light.get("min").getAsInt() : 0;
            lightMax = light.has("max") ? light.get("max").getAsInt() : 15;
        }

        int heightMin = Integer.MIN_VALUE, heightMax = Integer.MAX_VALUE;
        if (obj.has("height")) {
            var height = obj.getAsJsonObject("height");
            heightMin = height.has("min") ? height.get("min").getAsInt() : Integer.MIN_VALUE;
            heightMax = height.has("max") ? height.get("max").getAsInt() : Integer.MAX_VALUE;
        }

        var conditions = EnumSet.noneOf(SpawnCondition.class);
        if (obj.has("conditions")) {
            for (JsonElement e : obj.getAsJsonArray("conditions")) {
                var raw = e.getAsString().toUpperCase().replace('-', '_');
                try {
                    conditions.add(SpawnCondition.valueOf(raw));
                } catch (IllegalArgumentException ex) {
                    Decilib.LOGGER.warn("[deci-lib] Spawn config '{}' mob '{}': unknown condition '{}', ignoring",
                            source, mobId, e.getAsString());
                }
            }
        }

        return new SpawnEntryConfig(weight, minGroup, maxGroup, List.copyOf(biomes), dimension,
                lightMin, lightMax, heightMin, heightMax,
                conditions.isEmpty() ? Set.of() : Set.copyOf(conditions));
    }

    private static SpawnGroup parseSpawnGroup(String name, String source) {
        return switch (name.toLowerCase()) {
            case "creature"        -> SpawnGroup.CREATURE;
            case "ambient"         -> SpawnGroup.AMBIENT;
            case "water_creature"  -> SpawnGroup.WATER_CREATURE;
            case "water_ambient"   -> SpawnGroup.WATER_AMBIENT;
            case "misc"            -> SpawnGroup.MISC;
            case "monster"         -> SpawnGroup.MONSTER;
            default -> {
                Decilib.LOGGER.warn("[deci-lib] Spawn config '{}': unknown category '{}', defaulting to 'monster'",
                        source, name);
                yield SpawnGroup.MONSTER;
            }
        };
    }
}
