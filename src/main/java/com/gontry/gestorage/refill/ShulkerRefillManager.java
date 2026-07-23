package com.gontry.gestorage.refill;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.IdentityHashMap;

public class ShulkerRefillManager {
	private static final IdentityHashMap<Item, Boolean> SHULKER_CACHE = new IdentityHashMap<>();

	public static boolean isShulkerBox(ItemStack stack) {
		Item item = stack.getItem();
		Boolean cached = SHULKER_CACHE.get(item);
		if (cached != null) return cached;

		boolean result = false;
		if (item instanceof BlockItem blockItem) {
			String path = Registries.BLOCK.getId(blockItem.getBlock()).getPath();
			result = path.contains("shulker_box");
		}
		SHULKER_CACHE.put(item, result);
		return result;
	}

	public static void invalidateCache() {
		SHULKER_CACHE.clear();
	}
}
