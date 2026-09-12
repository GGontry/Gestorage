package com.gontry.gestorage.client;

import net.minecraft.item.ItemStack;

import java.util.Arrays;

public class ClientToolWheelState {
	public static final ItemStack[] STACKS = new ItemStack[9];
	public static volatile boolean autoTool = false;
	public static volatile boolean wheelActive = false;

	private ClientToolWheelState() {}

	static {
		Arrays.fill(STACKS, ItemStack.EMPTY);
	}

	public static void apply(ItemStack[] stacks, boolean autoTool) {
		for (int i = 0; i < 9; i++) {
			STACKS[i] = (stacks != null && i < stacks.length) ? stacks[i] : ItemStack.EMPTY;
		}
		ClientToolWheelState.autoTool = autoTool;
	}
}