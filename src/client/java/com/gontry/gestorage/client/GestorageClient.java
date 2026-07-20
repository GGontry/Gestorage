package com.gontry.gestorage.client;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.ModMenus;
import com.gontry.gestorage.refill.ShulkerLinkManager;
import com.gontry.gestorage.screen.ExtraLargeEnderScreen;
import com.gontry.gestorage.screen.LargeEnderScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class GestorageClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModConfig.load();
		ShulkerLinkManager.load();
		GestorageKeybinds.register();
		ShulkerRefillKeybinds.register();
		ShulkerRefillTickHandler.register();
		ModNetworkingClient.register();

		HandledScreens.register(ModMenus.LARGE_ENDER, LargeEnderScreen::new);
		HandledScreens.register(ModMenus.EXTRA_LARGE_ENDER, ExtraLargeEnderScreen::new);

		Gestorage.LOGGER.info("Gestorage client initialized!");
	}
}
