package com.gontry.gestorage.client;

import com.gontry.gestorage.ModConstants;
import net.minecraft.item.ItemStack;

import java.util.Arrays;

public class ClientToolWheelState {
	public static final ItemStack[] STACKS = new ItemStack[9];
	public static volatile boolean autoTool = false;
	public static volatile int defaultSlot = -1;
	public static volatile int enchPref = ModConstants.ENCH_PREF_NONE;
	public static volatile boolean wheelActive = false;
	public static volatile boolean wheelClickSuppress = false;

	private ClientToolWheelState() {}

	static {
		Arrays.fill(STACKS, ItemStack.EMPTY);
	}

	/** {@code defaultTool} is a reserved payload field, always sent empty by the server. */
	public static void apply(ItemStack[] stacks, boolean autoTool, ItemStack defaultTool, int defaultSlot) {
		for (int i = 0; i < 9; i++) {
			STACKS[i] = (stacks != null && i < stacks.length) ? stacks[i] : ItemStack.EMPTY;
		}
		ClientToolWheelState.autoTool = autoTool;
		ClientToolWheelState.defaultSlot = defaultSlot;
	}

	public static void applyEnchPref(int pref) {
		enchPref = (pref >= ModConstants.ENCH_PREF_NONE && pref <= ModConstants.ENCH_PREF_MAX)
				? pref : ModConstants.ENCH_PREF_NONE;
	}

	/** Next value of the None/Fortune/Silk Touch cycle, used by the keybind. */
	public static int nextEnchPref() {
		return (enchPref + 1) % (ModConstants.ENCH_PREF_MAX + 1);
	}
}