package net.dacommander31.lodestone_di;

import net.dacommander31.lodestone_di.integration.LodestoneDatapackInterpreter;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LodestoneDatapackIntegration implements ModInitializer {
	public static final String MOD_ID = "lodestone_di";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		LOGGER.info(FabricLoader.getInstance().getGameDir().toString());

		LodestoneDatapackInterpreter.initialize();

	}
}