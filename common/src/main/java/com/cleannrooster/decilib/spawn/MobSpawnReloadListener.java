package com.cleannrooster.decilib.spawn;

import com.cleannrooster.decilib.Decilib;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;


/**
 * Populates MobSpawnRegistry from server data resources on load and on /reload.
 *
 * <p>Implements vanilla {@link SynchronousResourceReloader} so it works on both
 * platforms without Fabric-specific interfaces:
 * <ul>
 *   <li>Fabric: wrapped with a {@code SimpleSynchronousResourceReloadListener} adapter
 *       (adds getFabricId()) in DecilibFabric.</li>
 *   <li>NeoForge: passed directly to AddReloadListenerEvent, which accepts
 *       PreparableReloadListener (SynchronousResourceReloader extends it).</li>
 * </ul>
 */
public class MobSpawnReloadListener implements SynchronousResourceReloader {

    /** Fabric adapter uses this; keep it accessible. */
    public static final Identifier ID = Identifier.of(Decilib.MOD_ID, "mob_spawns");

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
