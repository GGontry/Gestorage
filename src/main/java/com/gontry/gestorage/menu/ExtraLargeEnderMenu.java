package com.gontry.gestorage.menu;

import com.gontry.gestorage.ModConstants;
import com.gontry.gestorage.ModMenus;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class ExtraLargeEnderMenu extends ScreenHandler {
	private static final int CHEST_COLS = 19;
	private static final int CHEST_ROWS = 12;
	private static final int CHEST_SIZE = ModConstants.EXTRA_LARGE_ENDER_SIZE;
	private static final int PLAYER_INV_START = CHEST_SIZE;
	private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

	private static final int CHEST_X = 7;
	private static final int CHEST_Y = 17;
	private static final int PLAYER_INV_X = 97;
	private static final int PLAYER_INV_Y = 246;
	private static final int HOTBAR_Y = 304;

	private static final int INITIAL_SYNC_DELAY_TICKS = ModConstants.MAX_SYNC_DELAY_TICKS;

	private final Inventory inventory;
	private int ticksSinceOpen = 0;
	private boolean initialSyncDone = false;
	private boolean syncEnabled = false;

	public ExtraLargeEnderMenu(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, new net.minecraft.inventory.SimpleInventory(CHEST_SIZE));
	}

	public ExtraLargeEnderMenu(int syncId, PlayerInventory playerInventory, Inventory inventory) {
		super(ModMenus.EXTRA_LARGE_ENDER, syncId);
		checkSize(inventory, CHEST_SIZE);
		this.inventory = inventory;
		inventory.onOpen(playerInventory.player);

		for (int row = 0; row < CHEST_ROWS; row++) {
			for (int col = 0; col < CHEST_COLS; col++) {
				this.addSlot(new Slot(inventory, col + row * CHEST_COLS, CHEST_X + col * 18, CHEST_Y + row * 18));
			}
		}

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
			}
		}

		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y));
		}

		this.disableSyncing();
	}

	@Override
	public void sendContentUpdates() {
		if (!initialSyncDone) {
			ticksSinceOpen++;
			if (ticksSinceOpen == INITIAL_SYNC_DELAY_TICKS) {
				this.enableSyncing();
				this.updateToClient();
				initialSyncDone = true;
			}
			return;
		}

		super.sendContentUpdates();
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
