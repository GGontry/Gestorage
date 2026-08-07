package com.gontry.gestorage.client;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.ModMenus;
import com.gontry.gestorage.client.config.ModuleConfig;
import com.gontry.gestorage.config.ShulkerStackServerConfig;
import com.gontry.gestorage.network.ModNetworking;
import com.gontry.gestorage.refill.ShulkerLinkManager;
import com.gontry.gestorage.screen.ExtraLargeEnderScreen;
import com.gontry.gestorage.screen.LargeEnderScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class GestorageClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModuleConfig.initialize();
		ShulkerStackServerConfig.load();
		ShulkerLinkManager.load();

		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_CONFIG_SCREEN, OpenConfigScreenS2CPacket::handle);

		GestorageKeybinds.register();
		ModNetworkingClient.register();
		HandledScreens.register(ModMenus.LARGE_ENDER, LargeEnderScreen::new);
		HandledScreens.register(ModMenus.EXTRA_LARGE_ENDER, ExtraLargeEnderScreen::new);

		ShulkerRefillKeybinds.register();
		ShulkerRefillTickHandler.register();

		Gestorage.LOGGER.info("Gestorage client initialized!");
	}
}
