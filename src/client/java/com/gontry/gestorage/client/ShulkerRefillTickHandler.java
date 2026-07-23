package com.gontry.gestorage.client;

import com.gontry.gestorage.refill.ShulkerLink;
import com.gontry.gestorage.refill.ShulkerLinkManager;
import com.gontry.gestorage.screen.ExtraLargeEnderScreen;
import com.gontry.gestorage.screen.LargeEnderScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

import java.util.List;

public class ShulkerRefillTickHandler {
	private static int tickCounter = 0;
	private static final int TICK_INTERVAL = 1;

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;
			if (client.world == null) return;

			tickCounter++;
			if (tickCounter < TICK_INTERVAL) return;
			tickCounter = 0;

			String worldKey = WorldKeyHelper.getFullWorldKey(client);
			if (worldKey == null) return;
			List<ShulkerLink> links = ShulkerLinkManager.getLinksForWorld(worldKey);
			if (links.isEmpty()) return;

			Inventory playerInv = client.player.getInventory();

			for (ShulkerLink link : links) {
				if (client.currentScreen instanceof HandledScreen<?> screen) {
					Slot targetSlot = findSlot(screen, link.targetSlot(), link.targetType());
					if (targetSlot != null) {
						ItemStack targetStack = targetSlot.getStack();
						if (targetStack.isEmpty() || targetStack.getCount() < 64) {
							sendRefill(link);
						}
						continue;
					}
				}

				if (link.targetType().equals("player")) {
					int idx = link.targetSlot();
					if (idx >= 0 && idx < playerInv.size()) {
						ItemStack stack = playerInv.getStack(idx);
						if (stack.isEmpty() || stack.getCount() < 64) {
							sendRefill(link);
						}
					}
				} else {
					sendRefill(link);
				}
			}
		});
	}

	private static Slot findSlot(HandledScreen<?> screen, int slotIndex, String type) {
		for (Slot slot : screen.getScreenHandler().slots) {
			String slotType = getSlotType(slot, screen);
			if (slotType != null && slot.getIndex() == slotIndex && slotType.equals(type)) {
				return slot;
			}
		}
		return null;
	}

	static String getSlotType(Slot slot, HandledScreen<?> screen) {
		if (slot.inventory instanceof PlayerInventory) return "player";
		if (slot.inventory instanceof net.minecraft.inventory.EnderChestInventory) return "ender_normal";

		if (screen instanceof LargeEnderScreen) {
			return "ender_large";
		}
		if (screen instanceof ExtraLargeEnderScreen) {
			return "ender_xlarge";
		}

		return null;
	}

	private static void sendRefill(ShulkerLink link) {
		ClientPlayNetworking.send(new com.gontry.gestorage.network.ModNetworking.RefillRequestC2S(
			link.sourceSlot(), link.sourceType(), link.targetSlot(), link.targetType()
		));
	}
}
