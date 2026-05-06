package com.cleannrooster.decilib;

import com.cleannrooster.decilib.ai.DebugConfig;
import com.cleannrooster.decilib.builder.animation.MobAnimationDispatcherRegistry;
import com.cleannrooster.decilib.entity.ModEntities;
import com.cleannrooster.decilib.spawn.MobSpawnRegistrar;
import com.cleannrooster.decilib.spawn.MobSpawnReloadListener;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Decilib implements ModInitializer {
	public static final String MOD_ID = "deci-lib";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		AutoConfig.register(DeciLibConfig.class, GsonConfigSerializer::new);
		var config = AutoConfig.getConfigHolder(DeciLibConfig.class).getConfig();

		DebugConfig.setConfigValue(config.debugMode);

		// Entity types must be registered before the registry freezes.
		// Scans all mod containers so other mods can bundle deci-lib mob definitions.
		ModEntities.register();

		// SpawnRestriction uses a dynamic predicate that reads from MobSpawnRegistry
		// at spawn time, so it picks up any data loaded by the reload listener.
		MobSpawnRegistrar.registerSpawnRestrictions();

		// Single lazy BiomeModification — reads MobSpawnRegistry at biome-build time.
		// The reload listener runs before biomes are processed, so the data is ready.
		MobSpawnRegistrar.registerBiomeModification();

		// Reload listener populates MobSpawnRegistry from all sources (mods + datapacks)
		// on server start and on /reload. SERVER_DATA fires before biomes are built.
		ResourceManagerHelper.get(ResourceType.SERVER_DATA)
				.registerReloadListener(new MobSpawnReloadListener());

		if (FabricLoader.getInstance().isModLoaded("azurelib")) {
			registerAzurelibDispatcher();
		}

		LOGGER.info("deci-lib initialized.");
	}

	private static void registerAzurelibDispatcher() {
		MobAnimationDispatcherRegistry.register(
				new com.cleannrooster.decilib.builder.animation.AzurelibAnimationDispatcher());
	}
}
