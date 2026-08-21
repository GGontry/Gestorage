package com.gontry.gestorage.sorting;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InventorySorter {
	private InventorySorter() {}

	public static void sortInventory(Inventory inventory, int startSlot, int endSlot,
			boolean mergeStacks, boolean sortByName, boolean sortDescending) {
		List<ItemStack> stacks = new ArrayList<>();
		for (int i = startSlot; i <= endSlot; i++) {
			ItemStack stack = inventory.getStack(i);
			if (!stack.isEmpty()) {
				stacks.add(stack.copy());
			}
		}

		if (mergeStacks) {
			stacks = merge(stacks);
		}

		Comparator<ItemStack> comparator;
		if (sortByName) {
			comparator = Comparator.comparing((ItemStack s) ->
					Registries.ITEM.getId(s.getItem()).toString());
		} else {
			comparator = Comparator.comparingInt((ItemStack s) -> s.getCount());
		}
		if (sortDescending) {
			comparator = comparator.reversed();
		}
		stacks.sort(comparator);

		for (int i = startSlot; i <= endSlot; i++) {
			inventory.setStack(i, ItemStack.EMPTY);
		}

		int slotIndex = startSlot;
		for (ItemStack stack : stacks) {
			if (slotIndex > endSlot) break;
			inventory.setStack(slotIndex, stack);
			slotIndex++;
		}
	}

	private static List<ItemStack> merge(List<ItemStack> stacks) {
		List<ItemStack> result = new ArrayList<>();

		for (ItemStack stack : stacks) {
			int remaining = stack.getCount();

			for (ItemStack existing : result) {
				if (remaining <= 0) break;
				if (!ItemStack.areItemsAndComponentsEqual(existing, stack)) continue;

				int space = existing.getMaxCount() - existing.getCount();
				if (space <= 0) continue;

				int toMove = Math.min(space, remaining);
				existing.increment(toMove);
				remaining -= toMove;
			}

			if (remaining > 0) {
				ItemStack s = stack.copy();
				s.setCount(remaining);
				result.add(s);
			}
		}

		return result;
	}
}
