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
	private static final int MAX_BACKOFF = 20;
	private static int loopTick = 0;
	private static boolean lastScreenOpen = false;
	private static String lastScreenType = null;
	private static final Map<String, LinkState> linkStates = new HashMap<>();

	private static final class LinkState {
		int lastSeenCount = -1;
		int lastSent = -1;
		int backoff = 1;
		ItemStack cachedSource;
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!ModuleConfig.shulkerRefill().enabled()) return;
			if (client.player == null) return;
			if (client.world == null) return;

			loopTick++;

			String worldKey = WorldKeyHelper.getFullWorldKey(client);
			if (worldKey == null) return;
			List<ShulkerLink> links = ShulkerLinkManager.getLinksForWorld(worldKey);
			if (links.isEmpty()) {
				linkStates.clear();
				lastScreenOpen = false;
				lastScreenType = null;
				return;
			}

			Inventory playerInv = client.player.getInventory();
			HandledScreen<?> screen = client.currentScreen instanceof HandledScreen<?> s ? s : null;
			String screenType = screenType(screen);

			if (lastScreenOpen && lastScreenType != null && !lastScreenType.equals(screenType)) {
				topOffBlindTargets(links, lastScreenType);
			}
			lastScreenOpen = screen != null;
			lastScreenType = screenType;

			linkStates.keySet().removeIf(key -> links.stream().noneMatch(l -> linkKey(l).equals(key)));

			for (ShulkerLink link : links) {
				ItemStack source = findClientStack(screen, link.sourceSlot(), link.sourceType(), playerInv);
				ItemStack target = findClientStack(screen, link.targetSlot(), link.targetType(), playerInv);
				String key = linkKey(link);
				LinkState state = linkStates.computeIfAbsent(key, k -> new LinkState());

				if (source != null) {
					state.cachedSource = source.copy();
				}
				if (target == null) continue;

				int count = target.isEmpty() ? 0 : target.getCount();
				boolean consumed = count < state.lastSeenCount;
				state.lastSeenCount = count;

				if (!shouldRefill(target)) {
					state.backoff = 1;
					continue;
				}

				ItemStack effectiveSource = source != null ? source : state.cachedSource;
				if (effectiveSource != null && !sourceCanFill(effectiveSource, target) && !consumed) continue;

				int interval = consumed ? 1 : state.backoff;
				if (state.lastSent >= 0 && loopTick - state.lastSent < interval) continue;

				sendRefill(link);
				state.lastSent = loopTick;
				state.backoff = Math.min(state.backoff * 2, MAX_BACKOFF);
			}
		});
	}

	private static void topOffBlindTargets(List<ShulkerLink> links, String closedType) {
		for (ShulkerLink link : links) {
			if (!link.targetType().equals("player") && link.targetType().equals(closedType)) {
				sendRefill(link);
				LinkState state = linkStates.computeIfAbsent(linkKey(link), k -> new LinkState());
				state.lastSent = loopTick;
			}
		}
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
						&& target.getMaxCount() == stack.getMaxCount())) {
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

	private static String screenType(HandledScreen<?> screen) {
		if (screen == null) return null;
		for (Slot slot : screen.getScreenHandler().slots) {
			String type = getSlotType(slot, screen);
			if (type != null && !type.equals("player")) return type;
		}
		return null;
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
