package com.gontry.gestorage.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.PersistentStateManager;

import java.util.UUID;

public class EnhancedEnderChestInventory implements Inventory {
	private final Inventory vanillaEnderChest;
	private final SimpleInventory overflow;
	private final int totalSize;
	private final int overflowStart;
	private EnderOverflowState overflowState;
	private boolean dirty = false;

	public EnhancedEnderChestInventory(Inventory vanillaEnderChest, int totalSize) {
		this.vanillaEnderChest = vanillaEnderChest;
		this.totalSize = totalSize;
		this.overflowStart = vanillaEnderChest.size();
		this.overflow = new SimpleInventory(totalSize - vanillaEnderChest.size());
	}

	public EnhancedEnderChestInventory(Inventory vanillaEnderChest, int totalSize, PersistentStateManager stateManager, UUID playerUuid) {
		this.vanillaEnderChest = vanillaEnderChest;
		this.totalSize = totalSize;
		this.overflowStart = vanillaEnderChest.size();
		this.overflow = new SimpleInventory(totalSize - vanillaEnderChest.size());

		if (stateManager != null && playerUuid != null && totalSize > vanillaEnderChest.size()) {
			this.overflowState = EnderOverflowState.load(stateManager, playerUuid);
			if (this.overflowState.inventory != null) {
				for (int i = 0; i < Math.min(this.overflow.size(), this.overflowState.inventory.size()); i++) {
					this.overflow.setStack(i, this.overflowState.inventory.getStack(i));
				}
			}
			this.overflowState.inventory = this.overflow;
		}
	}

	@Override
	public int size() {
		return totalSize;
	}

	@Override
	public boolean isEmpty() {
		for (int i = 0; i < vanillaEnderChest.size(); i++) {
			if (!vanillaEnderChest.getStack(i).isEmpty()) return false;
		}
		for (int i = 0; i < overflow.size(); i++) {
			if (!overflow.getStack(i).isEmpty()) return false;
		}
		return true;
	}

	@Override
	public ItemStack getStack(int slot) {
		if (slot < overflowStart) {
			return vanillaEnderChest.getStack(slot);
		}
		return overflow.getStack(slot - overflowStart);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		dirty = true;
		if (slot < overflowStart) {
			return vanillaEnderChest.removeStack(slot, amount);
		}
		return overflow.removeStack(slot - overflowStart, amount);
	}

	@Override
	public ItemStack removeStack(int slot) {
		dirty = true;
		if (slot < overflowStart) {
			return vanillaEnderChest.removeStack(slot);
		}
		return overflow.removeStack(slot - overflowStart);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		dirty = true;
		if (slot < overflowStart) {
			vanillaEnderChest.setStack(slot, stack);
		} else {
			overflow.setStack(slot - overflowStart, stack);
		}
	}

	@Override
	public void markDirty() {
		dirty = true;
		vanillaEnderChest.markDirty();
		overflow.markDirty();
		if (overflowState != null) {
			overflowState.markDirty();
		}
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return true;
	}

	@Override
	public void onOpen(PlayerEntity player) {
		vanillaEnderChest.onOpen(player);
		overflow.onOpen(player);
	}

	@Override
	public void onClose(PlayerEntity player) {
		vanillaEnderChest.onClose(player);
		overflow.onClose(player);
	}

	@Override
	public void clear() {
		dirty = true;
		vanillaEnderChest.clear();
		overflow.clear();
	}

	public boolean isDirty() {
		return dirty;
	}

	public void clearDirty() {
		dirty = false;
	}
}
