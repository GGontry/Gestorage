package com.gontry.gestorage.client;

import net.minecraft.item.ItemStack;

import java.util.Arrays;

public class ClientToolWheelState {
	public static final ItemStack[] STACKS = new ItemStack[9];
	public static volatile boolean autoTool = false;
	public static volatile ItemStack defaultTool = ItemStack.EMPTY;
	public static volatile int defaultSlot = -1;
	public static volatile boolean wheelActive = false;
	public static volatile boolean wheelClickSuppress = false;

	private ClientToolWheelState() {}

	static {
		Arrays.fill(STACKS, ItemStack.EMPTY);
	}

	public static void apply(ItemStack[] stacks, boolean autoTool, ItemStack defaultTool, int defaultSlot) {
		for (int i = 0; i < 9; i++) {
			STACKS[i] = (stacks != null && i < stacks.length) ? stacks[i] : ItemStack.EMPTY;
		}
		ClientToolWheelState.autoTool = autoTool;
		ClientToolWheelState.defaultTool = defaultTool == null ? ItemStack.EMPTY : defaultTool;
		ClientToolWheelState.defaultSlot = defaultSlot;
	}
}