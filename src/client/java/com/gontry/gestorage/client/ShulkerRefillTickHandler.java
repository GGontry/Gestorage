package com.gontry.gestorage.client;

import com.gontry.gestorage.client.config.ModuleConfig;
import com.gontry.gestorage.refill.ShulkerLink;
import com.gontry.gestorage.refill.ShulkerLinkManager;
import com.gontry.gestorage.refill.ShulkerRefillManager;
import com.gontry.gestorage.screen.InventoryTypeProvider;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShulkerRefillTickHandler {
	private static int tickCounter = 0;
	private static int loopTick = 0;
	private static final int TICK_INTERVAL = 1;
	private static final int BLIND_THROTTLE = 20;
	private static final Map<String, Integer> lastSentTick = new HashMap<>();

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!ModuleConfig.shulkerRefill().enabled()) return;
			if (client.player == null) return;
			if (client.world == null) return;

			tickCounter++;
			if (tickCounter < TICK_INTERVAL) return;
			tickCounter = 0;
			loopTick++;

			String worldKey = WorldKeyHelper.getFullWorldKey(client);
			if (worldKey == null) return;
			List<ShulkerLink> links = ShulkerLinkManager.getLinksForWorld(worldKey);
			if (links.isEmpty()) return;

			Inventory playerInv = client.player.getInventory();
			HandledScreen<?> screen = client.currentScreen instanceof HandledScreen<?> s ? s : null;

			lastSentTick.keySet().removeIf(key -> links.stream().noneMatch(l -> linkKey(l).equals(key)));

			for (ShulkerLink link : links) {
				ItemStack source = findClientStack(screen, link.sourceSlot(), link.sourceType(), playerInv);
				ItemStack target = findClientStack(screen, link.targetSlot(), link.targetType(), playerInv);

				if (target != null && !shouldRefill(target)) continue;
				if (source != null && !sourceCanFill(source, target)) continue;

				int interval = source == null || target == null ? BLIND_THROTTLE : 1;
				String key = linkKey(link);
				int last = lastSentTick.getOrDefault(key, -1);
				if (last >= 0 && loopTick - last < interval) continue;

				sendRefill(link);
				lastSentTick.put(key, loopTick);
			}
		});
	}

	private static boolean shouldRefill(ItemStack target) {
		return target.isEmpty() || target.getCount() < effectiveThreshold();
	}

	private static int effectiveThreshold() {
		int threshold = ModuleConfig.shulkerRefill().refillThreshold();
		return threshold > 0 ? threshold : 64;
	}

	private static boolean sourceCanFill(ItemStack source, ItemStack target) {
		if (!ShulkerRefillManager.isShulkerBox(source)) return false;
		ContainerComponent container = source.get(DataComponentTypes.CONTAINER);
		if (container == null) return false;
		for (ItemStack stack : container.streamNonEmpty().toList()) {
			if (target == null || target.isEmpty()
					|| (target.isOf(stack.getItem())
						&& target.getCount() < target.getMaxCount()
						&& ItemStack.areItemsAndComponentsEqual(target, stack))) {
				return true;
			}
		}
		return false;
	}

	private static ItemStack findClientStack(HandledScreen<?> screen, int slot, String type, Inventory playerInv) {
		if (screen != null) {
			Slot found = findSlot(screen, slot, type);
			if (found != null) return found.getStack();
		}
		if (type.equals("player")) {
			if (slot >= 0 && slot < playerInv.size()) return playerInv.getStack(slot);
		}
		return null;
	}

	private static String linkKey(ShulkerLink link) {
		return link.sourceSlot() + ":" + link.sourceType() + ">" + link.targetSlot() + ":" + link.targetType();
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

		if (screen instanceof InventoryTypeProvider provider) {
			return provider.getInventoryType();
		}

		return null;
	}

	private static void sendRefill(ShulkerLink link) {
		ClientPlayNetworking.send(new com.gontry.gestorage.network.ModNetworking.RefillRequestC2S(
			link.sourceSlot(), link.sourceType(), link.targetSlot(), link.targetType()
		));
	}
}
