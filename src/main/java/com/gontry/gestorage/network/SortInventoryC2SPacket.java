package com.gontry.gestorage.network;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.menu.ExtraLargeEnderMenu;
import com.gontry.gestorage.menu.LargeEnderMenu;
import com.gontry.gestorage.sorting.InventorySorter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public class SortInventoryC2SPacket {
	public static void handle(ModNetworking.SortInventoryC2S payload, ServerPlayNetworking.Context ctx) {
		ctx.server().execute(() -> {
			ServerPlayerEntity player = ctx.player();
			if (player == null) return;

			ScreenHandler screenHandler = player.currentScreenHandler;

			int endSlot = getContainerEndSlot(screenHandler);
			if (endSlot < 0) {
				Gestorage.LOGGER.debug("[Sort] Unknown container type for player {}", player.getUuid());
				return;
			}

			Inventory inventory = screenHandler.getSlot(0).inventory;

			InventorySorter.sortInventory(inventory, 0, endSlot,
					payload.mergeStacks(), payload.sortByName(), payload.sortDescending());

			inventory.markDirty();

			Gestorage.LOGGER.debug("[Sort] Sorted container for player {}", player.getName().getString());
		});
	}

	private static int getContainerEndSlot(ScreenHandler screenHandler) {
		if (screenHandler instanceof GenericContainerScreenHandler handler) {
			return handler.getRows() * 9 - 1;
		} else if (screenHandler instanceof LargeEnderMenu) {
			return 53;
		} else if (screenHandler instanceof ExtraLargeEnderMenu) {
			return 227;
		}
		return -1;
	}
}
