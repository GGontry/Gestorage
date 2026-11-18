package com.gontry.gestorage.client;

import net.minecraft.item.ItemStack;

import java.util.List;

/** Latest inspector reply and whether the preview key is currently active. */
public class ClientInspectorState {
	public static volatile boolean active = false;
	/** True while waiting for the first reply of a target with no cached data. */
	public static volatile boolean loading = false;
	public static volatile int expectedRequestId = -1;
	public static volatile String title = "";
	public static volatile List<ItemStack> stacks = List.of();
	public static volatile List<String> infoLines = List.of();
	public static volatile ItemStack icon = ItemStack.EMPTY;

	private ClientInspectorState() {}

	public static void apply(String title, List<ItemStack> stacks, List<String> infoLines, ItemStack icon) {
		ClientInspectorState.title = title;
		ClientInspectorState.stacks = List.copyOf(stacks);
		ClientInspectorState.infoLines = List.copyOf(infoLines);
		ClientInspectorState.icon = icon.copy();
		loading = false;
	}

	public static void clear() {
		title = "";
		stacks = List.of();
		infoLines = List.of();
		icon = ItemStack.EMPTY;
		loading = false;
	}

	public static void reset() {
		active = false;
		expectedRequestId = -1;
		clear();
	}
}
