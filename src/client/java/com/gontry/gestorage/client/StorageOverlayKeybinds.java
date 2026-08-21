package com.gontry.gestorage.client;

import com.gontry.gestorage.client.config.ModuleConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;

import java.util.List;

public class StorageOverlayKeybinds {
	private record Option(String label, java.util.function.Supplier<String> key,
			java.util.function.BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {}

	private static final List<Option> OPTIONS = List.of(
			new Option("Storage Overlay", () -> ModuleConfig.storageOverlay().toggleEnabledKey(),
					() -> ModuleConfig.storageOverlay().enabled(), v -> ModuleConfig.storageOverlay().enabled(v)),
			new Option("Inventory Name", () -> ModuleConfig.storageOverlay().toggleInventoryNameKey(),
					() -> ModuleConfig.storageOverlay().showInventoryName(), v -> ModuleConfig.storageOverlay().showInventoryName(v)),
			new Option("Item Name", () -> ModuleConfig.storageOverlay().toggleItemNameKey(),
					() -> ModuleConfig.storageOverlay().showItemName(), v -> ModuleConfig.storageOverlay().showItemName(v)),
			new Option("Item Icon", () -> ModuleConfig.storageOverlay().toggleItemIconKey(),
					() -> ModuleConfig.storageOverlay().showItemIcon(), v -> ModuleConfig.storageOverlay().showItemIcon(v)),
			new Option("Stacks", () -> ModuleConfig.storageOverlay().toggleStackCountKey(),
					() -> ModuleConfig.storageOverlay().showStackCount(), v -> ModuleConfig.storageOverlay().showStackCount(v)),
			new Option("Items", () -> ModuleConfig.storageOverlay().toggleItemCountKey(),
					() -> ModuleConfig.storageOverlay().showItemCount(), v -> ModuleConfig.storageOverlay().showItemCount(v)),
			new Option("Free Slots", () -> ModuleConfig.storageOverlay().toggleFreeSlotsKey(),
					() -> ModuleConfig.storageOverlay().showFreeSlots(), v -> ModuleConfig.storageOverlay().showFreeSlots(v))
	);

	private static final boolean[] prev = new boolean[OPTIONS.size()];

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			long handle = client.getWindow() != null ? client.getWindow().getHandle() : 0;
			boolean active = client.player != null
					&& handle != 0
					&& client.currentScreen == null;

			for (int i = 0; i < OPTIONS.size(); i++) {
				Option option = OPTIONS.get(i);
				String key = option.key().get();
				boolean pressed = active && !key.isEmpty()
						&& KeybindHelper.isPressed(key, handle);
				if (pressed && !prev[i]) {
					boolean newState = !option.getter().getAsBoolean();
					option.setter().accept(newState);
					ModuleConfig.storageOverlay().save();
					client.player.sendMessage(Text.literal(
							"§7" + option.label() + ": " + (newState ? "§aON" : "§cOFF")), true);
				}
				prev[i] = pressed;
			}
		});
	}
}
