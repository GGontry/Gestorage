package com.gontry.gestorage.shulker;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Splits empty-shulker stacks that exceed the vanilla limit when player stacking
 * is off.
 *
 * <p>Turning the rule off does not touch stacks that already exist, so a player
 * could keep moving a 64-shulker stack through GUIs and containers even though
 * the new limit is 1. The same happens when a player joins a world whose rule is
 * already off with legacy stacks in their inventory. This class restores the
 * vanilla limit by keeping one shulker in the original slot, filling empty slots
 * with singles, and dropping whatever does not fit at the player's feet.
 */
public final class ShulkerStackNormalizer {
	private ShulkerStackNormalizer() {}

	public static void normalize(ServerPlayerEntity player) {
		if (ShulkerStackRule.enabled()) return;

		PlayerInventory inventory = player.getInventory();
		// Main inventory and hotbar (0-35), then offhand (40). Armor slots cannot
		// hold shulkers, so they are never used as overflow.
		normalizeRange(player, inventory, 0, 36);
		normalizeRange(player, inventory, 40, 41);
		normalizeCursor(player);

		EnderChestInventory enderChest = player.getEnderChestInventory();
		normalizeRange(player, enderChest, 0, enderChest.size());
	}

	/** Normalizes one already-created inventory, such as the extended ender chest. */
	public static void normalize(PlayerEntity player, Inventory inventory) {
		if (ShulkerStackRule.enabled()) return;
		normalizeRange(player, inventory, 0, inventory.size());
	}

	private static void normalizeRange(PlayerEntity player, Inventory inventory, int start, int end) {
		boolean changed = false;
		for (int slot = start; slot < end; slot++) {
			ItemStack stack = inventory.getStack(slot);
			if (stack.isEmpty() || !ShulkerStackHelper.isEmptyShulker(stack)) continue;

			int limit = ShulkerStackHelper.vanillaMaxCount(stack);
			if (stack.getCount() <= limit) continue;

			int extra = stack.getCount() - limit;
			ItemStack split = stack.copyWithCount(limit);
			inventory.setStack(slot, split);
			changed = true;

			for (int target = start; target < end && extra > 0; target++) {
				if (target == slot || !inventory.getStack(target).isEmpty()) continue;
				int moved = Math.min(extra, limit);
				inventory.setStack(target, split.copyWithCount(moved));
				extra -= moved;
			}

			while (extra > 0) {
				int drop = Math.min(extra, limit);
				player.dropItem(split.copyWithCount(drop), false);
				extra -= drop;
			}
		}
		if (changed) inventory.markDirty();
	}

	private static void normalizeCursor(ServerPlayerEntity player) {
		ItemStack cursor = player.currentScreenHandler.getCursorStack();
		if (cursor.isEmpty() || !ShulkerStackHelper.isEmptyShulker(cursor)) return;

		int limit = ShulkerStackHelper.vanillaMaxCount(cursor);
		if (cursor.getCount() <= limit) return;

		int extra = cursor.getCount() - limit;
		ItemStack limited = cursor.copyWithCount(limit);
		player.currentScreenHandler.setCursorStack(limited);

		while (extra > 0) {
			int drop = Math.min(extra, limit);
			player.dropItem(limited.copyWithCount(drop), false);
			extra -= drop;
		}
	}
}
