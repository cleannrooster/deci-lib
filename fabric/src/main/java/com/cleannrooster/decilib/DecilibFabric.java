package com.cleannrooster.decilib;

import com.cleannrooster.decilib.builder.animation.AzurelibAnimationDispatcher;
import com.cleannrooster.decilib.builder.animation.MobAnimationDispatcherRegistry;
import com.cleannrooster.decilib.entity.ModEntities;
import com.cleannrooster.decilib.spawn.MobSpawnRegistrar;
import com.cleannrooster.decilib.spawn.MobSpawnReloadListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.SpawnSettings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DecilibFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Decilib.init();

        // Collect all mod JAR root paths for entity type scanning.
        List<Path> roots = new ArrayList<>();
        for (var container : FabricLoader.getInstance().getAllMods()) {
            roots.addAll(container.getRootPaths());
        }

        // Phase 1: register entity types.
        ModEntities.registerEntityTypes(roots, FabricLoader.getInstance()::isModLoaded);

        // Phase 2: register spawn eggs (immediately after entities on Fabric).
        ModEntities.registerSpawnEggs();

        // Register entity attributes via Fabric API.
        ModEntities.getAttributeBuilders().forEach(FabricDefaultAttributeRegistry::register);

        // Add spawn eggs to the creative tab.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries ->
                ModEntities.getSpawnEggs().forEach(egg -> entries.add(new ItemStack(egg))));

        // SpawnRestriction — vanilla, works on both platforms.
        MobSpawnRegistrar.registerSpawnRestrictions();

        // Biome modification — Fabric API.
        BiomeModifications.create(Identifier.of(Decilib.MOD_ID, "mob_spawns"))
                .add(ModificationPhase.ADDITIONS, ctx -> true, (ctx, mutable) -> {
                    for (var config : com.cleannrooster.decilib.spawn.MobSpawnRegistry.all()) {
                        var rawType = ModEntities.getDataDrivenMobs().get(config.mobId());
                        if (rawType == null) {
                            Decilib.LOGGER.warn("[deci-lib] Biome modification: entity '{}' not registered, skipping",
                                    config.mobId());
                            continue;
                        }
                        @SuppressWarnings("unchecked")
                        var type = (net.minecraft.entity.EntityType<? extends MobEntity>) rawType;
                        for (var entry : config.entries()) {
                            if (!MobSpawnRegistrar.biomeMatchesEntry(ctx.getBiomeRegistryEntry(), entry)) continue;
                            // Dimension filter at biome-build time via BiomeSelectionContext.
                            if (entry.dimension() != null && !dimensionMatches(ctx, entry.dimension())) continue;
                            mutable.getSpawnSettings().addSpawn(
                                    config.spawnGroup(),
                                    new SpawnSettings.SpawnEntry(
                                            type, entry.weight(), entry.minGroupSize(), entry.maxGroupSize()));
                        }
                    }
                });

        // Reload listener — populates MobSpawnRegistry from data resources.
        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
                .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                    private final MobSpawnReloadListener delegate = new MobSpawnReloadListener();
                    @Override public Identifier getFabricId() { return MobSpawnReloadListener.ID; }
                    @Override public void reload(ResourceManager manager) { delegate.reload(manager); }
                });

        // AzureLib animation dispatcher (optional dependency).
        if (FabricLoader.getInstance().isModLoaded("azurelib")) {
            MobAnimationDispatcherRegistry.register(new AzurelibAnimationDispatcher());
        }

        Decilib.LOGGER.info("deci-lib initialized (Fabric).");
    }

    private static boolean dimensionMatches(net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext ctx, String dim) {
        return switch (dim) {
            case "minecraft:overworld"  -> ctx.hasTag(
                    net.minecraft.registry.tag.TagKey.of(net.minecraft.registry.RegistryKeys.BIOME,
                            Identifier.of("minecraft", "is_overworld")));
            case "minecraft:the_nether" -> ctx.hasTag(
                    net.minecraft.registry.tag.TagKey.of(net.minecraft.registry.RegistryKeys.BIOME,
                            Identifier.of("minecraft", "is_nether")));
            case "minecraft:the_end"    -> ctx.hasTag(
                    net.minecraft.registry.tag.TagKey.of(net.minecraft.registry.RegistryKeys.BIOME,
                            Identifier.of("minecraft", "is_end")));
            default -> {
                Decilib.LOGGER.warn("[deci-lib] Unknown dimension '{}' in spawn config, ignoring", dim);
                yield true;
            }
        };
    }
}
