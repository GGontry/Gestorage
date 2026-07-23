package com.gontry.gestorage.menu;

import com.gontry.gestorage.ModConstants;
import com.gontry.gestorage.ModMenus;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class LargeEnderMenu extends ScreenHandler {
	private static final int CHEST_SIZE = ModConstants.LARGE_ENDER_SIZE;
	private static final int PLAYER_INV_START = CHEST_SIZE;
	private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

	private final Inventory inventory;

	public LargeEnderMenu(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, new net.minecraft.inventory.SimpleInventory(CHEST_SIZE));
	}

	public LargeEnderMenu(int syncId, PlayerInventory playerInventory, Inventory inventory) {
		super(ModMenus.LARGE_ENDER, syncId);
		checkSize(inventory, CHEST_SIZE);
		this.inventory = inventory;
		inventory.onOpen(playerInventory.player);

		for (int row = 0; row < 6; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18));
			}
		}

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 139 + row * 18));
			}
		}

		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 197));
		}
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasStack()) return ItemStack.EMPTY;

		ItemStack stack = slot.getStack();
		ItemStack original = stack.copy();

		if (slotIndex < CHEST_SIZE) {
			if (!this.insertItem(stack, PLAYER_INV_START, PLAYER_INV_END, true)) {
				return ItemStack.EMPTY;
			}
		} else {
			if (!this.insertItem(stack, 0, CHEST_SIZE, false)) {
				return ItemStack.EMPTY;
			}
		}

		if (stack.isEmpty()) {
			slot.setStack(ItemStack.EMPTY);
		} else {
			slot.markDirty();
		}

		return original;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return this.inventory.canPlayerUse(player);
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		this.inventory.onClose(player);
	}

	public Inventory getInventory() {
		return inventory;
	}
}
