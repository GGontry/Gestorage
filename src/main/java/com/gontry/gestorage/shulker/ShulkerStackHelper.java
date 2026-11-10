package com.gontry.gestorage.shulker;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

/**
 * Shared predicates for the Stackable Shulkers rules.
 *
 * <p>Every call site that needs to know whether a stack is an <em>empty</em>
 * shulker, or what its vanilla limit is, goes through here so the item-entity,
 * hopper, GUI, Lithium and normalization paths can never disagree.
 */
public final class ShulkerStackHelper {
	private ShulkerStackHelper() {}

	/** The stack size the item would have without Gestorage's override. */
	public static int vanillaMaxCount(ItemStack stack) {
		return stack.getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 1);
	}

	public static boolean isShulkerBox(ItemStack stack) {
		return stack.getItem() instanceof BlockItem blockItem
				&& blockItem.getBlock() instanceof ShulkerBoxBlock;
	}

	/**
	 * Whether the stack is an empty shulker box. A shulker carrying a loot table
	 * ({@code CONTAINER_LOOT}) is treated as non-empty: placing it would generate
	 * contents, so it must keep the vanilla limit of 1.
	 */
	public static boolean isEmptyShulker(ItemStack stack) {
		if (!isShulkerBox(stack)) return false;
		if (stack.contains(DataComponentTypes.CONTAINER_LOOT)) return false;

		ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
		if (container == null) return true;
		for (ItemStack ignored : container.iterateNonEmpty()) {
			return false;
		}
		return true;
	}

	/**
	 * The stack limit Gestorage exposes for an empty shulker while stacking is on.
	 * A modded shulker that already allows more than one item keeps its own limit
	 * instead of being raised to 64; a normal (limit 1) shulker becomes 64.
	 */
	public static int stackedMaxCount(ItemStack stack) {
		int vanilla = vanillaMaxCount(stack);
		return vanilla <= 1 ? 64 : Math.min(vanilla, 64);
	}
}
