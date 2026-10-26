package com.gontry.gestorage.sorting;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InventorySorter {
	private InventorySorter() {}

	/**
	 * Sorts and optionally merges the items in {@code [startSlot, endSlot]}.
	 *
	 * <p>Slots in {@code lockedSlots} are invisible to the sort: their items are
	 * neither collected nor overwritten, and the sorted stacks are packed around
	 * them. Sorting an empty lock set behaves exactly like the original method.
	 */
	public static void sortInventory(Inventory inventory, int startSlot, int endSlot,
			boolean mergeStacks, boolean sortByName, boolean sortDescending, Set<Integer> lockedSlots) {
		boolean hasLocks = lockedSlots != null && !lockedSlots.isEmpty();
		List<ItemStack> stacks = new ArrayList<>();
		for (int i = startSlot; i <= endSlot; i++) {
			if (hasLocks && lockedSlots.contains(i)) continue;
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
			Map<ItemStack, String> names = new IdentityHashMap<>();
			for (ItemStack s : stacks) {
				names.put(s, Registries.ITEM.getId(s.getItem()).toString());
			}
			comparator = Comparator.comparing(names::get);
		} else {
			comparator = Comparator.comparingInt((ItemStack s) -> s.getCount());
		}
		if (sortDescending) {
			comparator = comparator.reversed();
		}
		stacks.sort(comparator);

		for (int i = startSlot; i <= endSlot; i++) {
			if (hasLocks && lockedSlots.contains(i)) continue;
			inventory.setStack(i, ItemStack.EMPTY);
		}

		int slotIndex = startSlot;
		for (ItemStack stack : stacks) {
			if (hasLocks) {
				while (slotIndex <= endSlot && lockedSlots.contains(slotIndex)) {
					slotIndex++;
				}
			}
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
