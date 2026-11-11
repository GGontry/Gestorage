package com.gontry.gestorage.inventory;

import net.minecraft.entity.ContainerUser;
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
	private final EnderOverflowState overflowState;
	private final PersistentStateManager stateManager;

	public EnhancedEnderChestInventory(Inventory vanillaEnderChest, int totalSize, PersistentStateManager stateManager, UUID playerUuid) {
		this.vanillaEnderChest = vanillaEnderChest;
		this.totalSize = totalSize;
		this.overflowStart = vanillaEnderChest.size();
		this.stateManager = stateManager;

		if (stateManager != null && playerUuid != null && totalSize > vanillaEnderChest.size()) {
			this.overflowState = EnderOverflowState.load(stateManager, playerUuid);
			int neededSize = totalSize - vanillaEnderChest.size();
			if (this.overflowState.inventory == null) {
				this.overflowState.inventory = new SimpleInventory(neededSize);
			} else if (this.overflowState.inventory.size() < neededSize) {
				SimpleInventory newInv = new SimpleInventory(neededSize);
				for (int i = 0; i < Math.min(newInv.size(), this.overflowState.inventory.size()); i++) {
					newInv.setStack(i, this.overflowState.inventory.getStack(i));
				}
				this.overflowState.inventory = newInv;
			}
			this.overflow = this.overflowState.inventory;
		} else {
			this.overflow = new SimpleInventory(totalSize - vanillaEnderChest.size());
			this.overflowState = null;
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
		if (slot < overflowStart) {
			return vanillaEnderChest.removeStack(slot, amount);
		}
		return overflow.removeStack(slot - overflowStart, amount);
	}

	@Override
	public ItemStack removeStack(int slot) {
		if (slot < overflowStart) {
			return vanillaEnderChest.removeStack(slot);
		}
		return overflow.removeStack(slot - overflowStart);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		if (slot < overflowStart) {
			vanillaEnderChest.setStack(slot, stack);
		} else {
			overflow.setStack(slot - overflowStart, stack);
		}
	}

	@Override
	public void markDirty() {
		vanillaEnderChest.markDirty();
		overflow.markDirty();
		if (overflowState != null) {
			overflowState.markDirty();
			// Throttled inside the state, so every wrapper of this player shares the
			// same window instead of saving once per (short-lived) wrapper.
			overflowState.flush(stateManager);
		}
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return true;
	}

	@Override
	public void onOpen(ContainerUser user) {
		vanillaEnderChest.onOpen(user);
		overflow.onOpen(user);
	}

	@Override
	public void onClose(ContainerUser user) {
		vanillaEnderChest.onClose(user);
		overflow.onClose(user);
		markDirty();
		if (overflowState != null) {
			overflowState.flushNow(stateManager);
		}
	}

	@Override
	public void clear() {
		vanillaEnderChest.clear();
		overflow.clear();
	}

	public Inventory getVanillaInventory() {
		return vanillaEnderChest;
	}

	public Inventory getOverflowInventory() {
		return overflow;
	}
}
