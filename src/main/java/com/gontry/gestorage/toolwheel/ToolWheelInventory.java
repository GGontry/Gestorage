package com.gontry.gestorage.toolwheel;

import com.gontry.gestorage.network.ToolWheelSyncS2CPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class ToolWheelInventory implements Inventory {
	private final ToolWheelState state;
	private final ServerPlayerEntity owner;

	public ToolWheelInventory(ToolWheelState state, ServerPlayerEntity owner) {
		this.state = state;
		this.owner = owner;
	}

	@Override
	public int size() {
		return ToolWheelState.SIZE;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : state.stacks) {
			if (!stack.isEmpty()) return false;
		}
		return true;
	}

	@Override
	public ItemStack getStack(int slot) {
		return state.stacks.get(slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		ItemStack stack = state.stacks.get(slot);
		if (stack.isEmpty()) return ItemStack.EMPTY;
		return stack.split(amount);
	}

	@Override
	public ItemStack removeStack(int slot) {
		ItemStack stack = state.stacks.get(slot);
		if (stack.isEmpty()) return ItemStack.EMPTY;
		state.stacks.set(slot, ItemStack.EMPTY);
		return stack;
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		state.stacks.set(slot, stack);
		if (!stack.isEmpty() && stack.getCount() > stack.getMaxCount()) {
			stack.setCount(stack.getMaxCount());
		}
	}

	@Override
	public void markDirty() {
		state.scheduleSave();
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return true;
	}

	@Override
	public void onClose(PlayerEntity player) {
		state.forceSave();
		ToolWheelSyncS2CPacket.sendTo(owner);
	}

	@Override
	public void clear() {
		for (int i = 0; i < size(); i++) {
			state.stacks.set(i, ItemStack.EMPTY);
		}
	}
}