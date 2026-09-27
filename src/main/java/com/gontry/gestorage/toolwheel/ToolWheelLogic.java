package com.gontry.gestorage.toolwheel;

import com.gontry.gestorage.ModConstants;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ToolWheelLogic {
	private ToolWheelLogic() {}

	public static boolean swapWithHand(ServerPlayerEntity player, int wheelSlot) {
		if (wheelSlot < 0 || wheelSlot >= ToolWheelState.SIZE) return false;
		ToolWheelState state = ToolWheelState.get(player);
		PlayerInventory inv = player.getInventory();
		ItemStack hand = inv.getStack(inv.selectedSlot);
		ItemStack wheel = state.stacks.get(wheelSlot);
		if (hand.isEmpty() && wheel.isEmpty()) return false;
		if (ItemStack.areEqual(hand, wheel)) return true;
		inv.setStack(inv.selectedSlot, wheel);
		state.stacks.set(wheelSlot, hand);
		inv.markDirty();
		state.scheduleSave();
		return true;
	}

	/**
	 * Pins the hotbar slot Auto Tool swaps into. {@link ModConstants#TOOL_SLOT_NONE}
	 * restores the vanilla behaviour of using the currently selected slot.
	 */
	public static void setDefaultSlot(ServerPlayerEntity player, int hotbarSlot) {
		if (hotbarSlot != ModConstants.TOOL_SLOT_NONE && !PlayerInventory.isValidHotbarIndex(hotbarSlot)) return;
		ToolWheelState state = ToolWheelState.get(player);
		state.defaultSlot = hotbarSlot;
		state.scheduleSave();
	}

	/**
	 * Hotbar slot Auto Tool must swap into for this player: the pinned slot when
	 * one was chosen, otherwise whatever the player has selected right now.
	 */
	public static int resolveToolSlot(ToolWheelState state, PlayerInventory inv) {
		return PlayerInventory.isValidHotbarIndex(state.defaultSlot) ? state.defaultSlot : inv.selectedSlot;
	}
}