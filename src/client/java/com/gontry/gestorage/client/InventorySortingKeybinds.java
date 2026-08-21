package com.gontry.gestorage.client;

import com.gontry.gestorage.client.config.ModuleConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;

import java.util.List;

public class InventorySortingKeybinds {
	private record Option(String label, java.util.function.Supplier<String> key,
			java.util.function.BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {}

	private static final List<Option> OPTIONS = List.of(
			new Option("Inventory Sorting", () -> ModuleConfig.inventorySorting().toggleEnabledKey(),
					() -> ModuleConfig.inventorySorting().enabled(), v -> ModuleConfig.inventorySorting().enabled(v)),
			new Option("Show Buttons", () -> ModuleConfig.inventorySorting().toggleShowButtonsKey(),
					() -> ModuleConfig.inventorySorting().showButtons(), v -> ModuleConfig.inventorySorting().showButtons(v)),
			new Option("Merge Stacks", () -> ModuleConfig.inventorySorting().toggleMergeStacksKey(),
					() -> ModuleConfig.inventorySorting().mergeStacks(), v -> ModuleConfig.inventorySorting().mergeStacks(v)),
			new Option("Sort By Name", () -> ModuleConfig.inventorySorting().toggleSortByNameKey(),
					() -> ModuleConfig.inventorySorting().sortByName(), v -> ModuleConfig.inventorySorting().sortByName(v)),
			new Option("Sort Descending", () -> ModuleConfig.inventorySorting().toggleSortDescendingKey(),
					() -> ModuleConfig.inventorySorting().sortDescending(), v -> ModuleConfig.inventorySorting().sortDescending(v)),
			new Option("Block Player Inventory", () -> ModuleConfig.inventorySorting().toggleBlockPlayerKey(),
					() -> ModuleConfig.inventorySorting().blockPlayer(), v -> ModuleConfig.inventorySorting().blockPlayer(v)),
			new Option("Block Ender Chest", () -> ModuleConfig.inventorySorting().toggleBlockEnderChestKey(),
					() -> ModuleConfig.inventorySorting().blockEnderChest(), v -> ModuleConfig.inventorySorting().blockEnderChest(v)),
			new Option("Block Shulker Box", () -> ModuleConfig.inventorySorting().toggleBlockShulkerBoxKey(),
					() -> ModuleConfig.inventorySorting().blockShulkerBox(), v -> ModuleConfig.inventorySorting().blockShulkerBox(v)),
			new Option("Block Chest/Barrel", () -> ModuleConfig.inventorySorting().toggleBlockGenericContainerKey(),
					() -> ModuleConfig.inventorySorting().blockGenericContainer(), v -> ModuleConfig.inventorySorting().blockGenericContainer(v))
	);

	private static boolean wasPressed = false;
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
					ModuleConfig.inventorySorting().save();
					client.player.sendMessage(Text.literal(
							"§7" + option.label() + ": " + (newState ? "§aON" : "§cOFF")), true);
				}
				prev[i] = pressed;
			}

			boolean pressed = false;
			if (ModuleConfig.inventorySorting().enabled()
					&& client.player != null
					&& client.getWindow() != null
					&& client.currentScreen instanceof HandledScreen<?> screen
					&& !InventorySortingRenderer.isInventoryBlocked(screen)) {
				pressed = handle != 0
						&& KeybindHelper.isPressed(ModuleConfig.inventorySorting().sortKey(), handle);
			}
			if (pressed && !wasPressed) {
				ModNetworkingClient.sendSortInventory(
						ModuleConfig.inventorySorting().mergeStacks(),
						ModuleConfig.inventorySorting().sortByName(),
						ModuleConfig.inventorySorting().sortDescending()
				);
			}
			wasPressed = pressed;
		});
	}
}
