package com.gontry.gestorage.client;

import com.gontry.gestorage.client.config.ModuleConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;

public class GestorageKeybinds {
	private static boolean wasPressed = false;
	private static boolean wasTogglePressed = false;

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			long handle = client.getWindow() != null ? client.getWindow().getHandle() : 0;
			boolean inGame = client.player != null
					&& client.currentScreen == null
					&& handle != 0;

			boolean pressed = inGame && ModuleConfig.enderChest().enabled()
					&& KeybindHelper.isPressed(ModuleConfig.enderChest().openEnderChestKey(), handle);
			if (pressed && !wasPressed) {
				ModNetworkingClient.sendOpenEnderChest();
			}
			wasPressed = pressed;

			boolean togglePressed = inGame && !ModuleConfig.enderChest().toggleEnabledKey().isEmpty()
					&& KeybindHelper.isPressed(ModuleConfig.enderChest().toggleEnabledKey(), handle);
			if (togglePressed && !wasTogglePressed) {
				boolean newState = !ModuleConfig.enderChest().enabled();
				ModuleConfig.enderChest().enabled(newState);
				ModuleConfig.enderChest().save();
				client.player.sendMessage(Text.literal(
						"§7Ender Key: " + (newState ? "§aON" : "§cOFF")), true);
			}
			wasTogglePressed = togglePressed;
		});
	}
}
