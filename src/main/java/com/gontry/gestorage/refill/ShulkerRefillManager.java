package com.gontry.gestorage.refill;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

public class ShulkerRefillManager {
	public static boolean isShulkerBox(ItemStack stack) {
		if (stack.getItem() instanceof BlockItem blockItem) {
			String path = Registries.BLOCK.getId(blockItem.getBlock()).getPath();
			return path.contains("shulker_box");
		}
		return false;
	}
}
