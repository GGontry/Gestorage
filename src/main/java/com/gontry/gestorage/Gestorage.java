package com.gontry.gestorage;

import com.gontry.gestorage.command.GestorageCommands;
import com.gontry.gestorage.config.CarefulBreakServerConfig;
import com.gontry.gestorage.config.ShulkerStackServerConfig;
import com.gontry.gestorage.network.CarefulBreakStateS2CPacket;
import com.gontry.gestorage.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
		ShulkerStackServerConfig.load();
		CarefulBreakServerConfig.load();

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				CarefulBreakStateS2CPacket.sendTo(handler.player));

		LOGGER.info("Gestorage initialized!");
	}
}
