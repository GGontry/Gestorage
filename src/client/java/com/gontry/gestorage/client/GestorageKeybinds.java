package com.gontry.gestorage.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;

public class GestorageKeybinds {
	private static boolean wasPressed = false;

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;
			if (client.getWindow() == null) return;
			if (client.currentScreen != null) return;

			long handle = client.getWindow().getHandle();
			if (handle == 0) return;

			boolean pressed = KeybindHelper.isPressed(ModConfig.get().getOpenEnderChestKey(), handle);
			if (pressed && !wasPressed) {
				ModNetworkingClient.sendOpenEnderChest();
			}
			wasPressed = pressed;
		});
	}
}
