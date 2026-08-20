package com.gontry.gestorage.client;

import com.gontry.gestorage.client.config.ModuleConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

public class InventorySortingKeybinds {
	private static boolean wasPressed = false;

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!ModuleConfig.inventorySorting().enabled()) return;
			if (client.player == null) return;
			if (client.getWindow() == null) return;
			if (!(client.currentScreen instanceof HandledScreen<?> screen)) return;

			long handle = client.getWindow().getHandle();
			if (handle == 0) return;

			boolean pressed = KeybindHelper.isPressed(ModuleConfig.inventorySorting().sortKey(), handle);
			if (pressed && !wasPressed) {
				if (!InventorySortingRenderer.isInventoryBlocked(screen)) {
					ModNetworkingClient.sendSortInventory(
							ModuleConfig.inventorySorting().mergeStacks(),
							ModuleConfig.inventorySorting().sortByName(),
							ModuleConfig.inventorySorting().sortDescending()
					);
				}
			}
			wasPressed = pressed;
		});
	}
}
