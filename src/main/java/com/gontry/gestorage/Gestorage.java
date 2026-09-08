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

/**
 * Fabric mod entry point (common side).
 *
 * New modules (e.g. a new inventory module) are wired from here on the common
 * side and from {@code GestorageClient} on the physical client side. Module
 * registration blocks are grouped so a module can be added or removed cleanly.
 */
public class Gestorage implements ModInitializer {
	public static final String MOD_ID = "gestorage";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 1. Game rules and menus first, so everything below can reference them.
		ModGameRules.register();
		ModMenus.register();
		// 2. Networking must be registered before any packet can arrive.
		ModNetworking.register();
		GestorageCommands.register();
		// 3. Server-side configs loaded before their first read.
		ShulkerStackServerConfig.load();
		CarefulBreakServerConfig.load();

		// Push the current Careful Break state to every player on join, so the
		// client always shows the server-authoritative configuration.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				CarefulBreakStateS2CPacket.sendTo(handler.player));

		LOGGER.info("Gestorage initialized!");
	}
}
