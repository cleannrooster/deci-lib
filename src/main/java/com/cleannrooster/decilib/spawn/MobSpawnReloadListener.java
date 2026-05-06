package com.cleannrooster.decilib.spawn;

import com.cleannrooster.decilib.Decilib;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;


public class MobSpawnReloadListener implements SimpleSynchronousResourceReloadListener {

    @Override
    public Identifier getFabricId() {
        return Identifier.of(Decilib.MOD_ID, "mob_spawns");
    }

    @Override
    public void reload(ResourceManager manager) {
        MobSpawnRegistry.clear();

        var resources = manager.findResources("mob_spawns",
                id -> id.getNamespace().equals(Decilib.MOD_ID) && id.getPath().endsWith(".json"));

        for (var entry : resources.entrySet()) {
            Identifier resourceId = entry.getKey();
            try (var reader = new InputStreamReader(entry.getValue().getInputStream())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                MobSpawnConfig config = MobSpawnConfigLoader.parse(json, resourceId.toString());
                MobSpawnRegistry.register(config);
                Decilib.LOGGER.info("[deci-lib] Loaded spawn config '{}' ({} entries)",
                        config.mobId(), config.entries().size());
            } catch (Exception e) {
                Decilib.LOGGER.error("[deci-lib] Failed to load spawn config {}: {}",
                        resourceId, e.getMessage(), e);
            }
        }
    }
}
