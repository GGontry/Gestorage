package com.gontry.gestorage.refill;

import com.gontry.gestorage.shulker.ShulkerStackHelper;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ShulkerRefillManager {
	private static final Map<Item, Boolean> SHULKER_CACHE = new ConcurrentHashMap<>();

	public static boolean isShulkerBox(ItemStack stack) {
		if (stack.isEmpty()) return false;
		Item item = stack.getItem();
		Boolean cached = SHULKER_CACHE.get(item);
		if (cached != null) return cached;

		boolean result = ShulkerStackHelper.isShulkerBox(stack)
				|| (item instanceof BlockItem blockItem && hasShulkerBoxPath(Registries.BLOCK.getId(blockItem.getBlock())));
		SHULKER_CACHE.put(item, result);
		return result;
	}

	/** Modded shulker boxes normally keep the vanilla name as their block id path. */
	private static boolean hasShulkerBoxPath(Identifier id) {
		String path = id.getPath();
		return path.equals("shulker_box")
				|| path.endsWith("_shulker_box")
				|| path.startsWith("shulker_box_");
	}
}
