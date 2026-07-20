package com.gontry.gestorage.inventory;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.UUID;

public class EnderOverflowState extends PersistentState {
	public SimpleInventory inventory;

	public EnderOverflowState() {
		this.inventory = new SimpleInventory(201);
	}

	public EnderOverflowState(SimpleInventory inventory) {
		this.inventory = inventory;
	}

	public static EnderOverflowState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		SimpleInventory inv = new SimpleInventory(nbt.getInt("Size"));
		if (nbt.contains("Items")) {
			NbtList items = nbt.getList("Items", 10);
			for (int i = 0; i < items.size(); i++) {
				NbtCompound itemNbt = items.getCompound(i);
				int slot = itemNbt.getInt("Slot");
				ItemStack stack = ItemStack.fromNbtOrEmpty(lookup, itemNbt);
				if (!stack.isEmpty()) {
					inv.setStack(slot, stack);
				}
			}
		}
		return new EnderOverflowState(inv);
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		nbt.putInt("Size", inventory.size());
		NbtList items = new NbtList();
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (!stack.isEmpty()) {
				NbtCompound itemNbt = (NbtCompound) stack.encodeAllowEmpty(lookup);
				itemNbt.putInt("Slot", i);
				items.add(itemNbt);
			}
		}
		nbt.put("Items", items);
		return nbt;
	}

	public static String getKey(UUID playerUuid) {
		return "gestorage_ender_overflow_" + playerUuid.toString();
	}

	public static EnderOverflowState load(PersistentStateManager manager, UUID playerUuid) {
		return manager.getOrCreate(
				new PersistentState.Type<>(
						EnderOverflowState::new,
						EnderOverflowState::fromNbt,
						null
				),
				getKey(playerUuid)
		);
	}
}
