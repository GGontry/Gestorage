package com.gontry.gestorage.network;

import com.gontry.gestorage.menu.ExtraLargeEnderMenu;
import com.gontry.gestorage.menu.LargeEnderMenu;
import com.gontry.gestorage.sorting.InventorySorter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class SortInventoryC2SPacket {
	public static void handle(ModNetworking.SortInventoryC2S payload, ServerPlayNetworking.Context ctx) {
		ctx.server().execute(() -> {
			ServerPlayerEntity player = ctx.player();
			if (player == null) return;

			ScreenHandler screenHandler = player.currentScreenHandler;

			int endSlot = getContainerEndSlot(screenHandler);
			if (endSlot < 0) {
				return;
			}

			Inventory inventory;
			int startSlot;
			if (screenHandler instanceof net.minecraft.screen.PlayerScreenHandler) {
				inventory = player.getInventory();
				startSlot = 9;
			} else {
				inventory = screenHandler.getSlot(0).inventory;
				startSlot = 0;
			}

			InventorySorter.sortInventory(inventory, startSlot, endSlot,
					payload.mergeStacks(), payload.sortByName(), payload.sortDescending());

			screenHandler.sendContentUpdates();

			player.sendMessage(Text.literal("§7Inventory sorted"), true);
		});
	}

	private static int getContainerEndSlot(ScreenHandler screenHandler) {
		if (screenHandler instanceof GenericContainerScreenHandler handler) {
			return handler.getRows() * 9 - 1;
		} else if (screenHandler instanceof net.minecraft.screen.ShulkerBoxScreenHandler) {
			return 26;
		} else if (screenHandler instanceof LargeEnderMenu) {
			return 53;
		} else if (screenHandler instanceof ExtraLargeEnderMenu) {
			return 227;
		} else if (screenHandler instanceof net.minecraft.screen.PlayerScreenHandler) {
			return 35;
		}
		return -1;
	}
}
