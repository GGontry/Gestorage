package com.gontry.gestorage.client;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.ModMenus;
import com.gontry.gestorage.client.config.ModuleConfig;
import com.gontry.gestorage.refill.ShulkerLinkManager;
import com.gontry.gestorage.screen.ExtraLargeEnderScreen;
import com.gontry.gestorage.screen.LargeEnderScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class GestorageClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModuleConfig.initialize();
		ShulkerLinkManager.load();

		if (ModuleConfig.enderChest().enabled()) {
			GestorageKeybinds.register();
			ModNetworkingClient.register();
			HandledScreens.register(ModMenus.LARGE_ENDER, LargeEnderScreen::new);
			HandledScreens.register(ModMenus.EXTRA_LARGE_ENDER, ExtraLargeEnderScreen::new);
		}

		if (ModuleConfig.shulkerRefill().enabled()) {
			ShulkerRefillKeybinds.register();
			ShulkerRefillTickHandler.register();
		}

		Gestorage.LOGGER.info("Gestorage client initialized!");
	}
}
