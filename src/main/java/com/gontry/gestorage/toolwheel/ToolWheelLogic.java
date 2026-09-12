package com.gontry.gestorage.toolwheel;

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
}