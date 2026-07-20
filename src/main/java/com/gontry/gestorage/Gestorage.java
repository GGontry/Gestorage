package com.gontry.gestorage;

import com.gontry.gestorage.command.GestorageCommands;
import com.gontry.gestorage.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Gestorage implements ModInitializer {
	public static final String MOD_ID = "gestorage";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModGameRules.register();
		ModMenus.register();
		ModNetworking.register();
		GestorageCommands.register();

		LOGGER.info("Gestorage initialized!");
	}
}
