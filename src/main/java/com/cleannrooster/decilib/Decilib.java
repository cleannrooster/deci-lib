package com.cleannrooster.decilib;

import com.cleannrooster.decilib.ai.DebugConfig;
import com.cleannrooster.decilib.builder.animation.MobAnimationDispatcherRegistry;
import com.cleannrooster.decilib.entity.ModEntities;
import com.cleannrooster.decilib.spawn.MobSpawnConfigLoader;
import com.cleannrooster.decilib.spawn.MobSpawnRegistrar;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
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
		ModEntities.register();
		MobSpawnConfigLoader.loadAll();
		MobSpawnRegistrar.registerAll();

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
