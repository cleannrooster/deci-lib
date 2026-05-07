package com.cleannrooster.decilib;

import com.cleannrooster.decilib.ai.DebugConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Decilib {
	public static final String MOD_ID = "decilib";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * Platform-neutral initialization: config registration and debug flag.
	 * Called by each platform entrypoint (DecilibFabric / DecilibNeoForge) before
	 * any entity or spawn registration work.
	 */
	public static void init() {
		AutoConfig.register(DeciLibConfig.class, GsonConfigSerializer::new);
		var config = AutoConfig.getConfigHolder(DeciLibConfig.class).getConfig();
		DebugConfig.setConfigValue(config.debugMode);
	}
}
