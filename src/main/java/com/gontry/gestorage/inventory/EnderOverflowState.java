package com.gontry.gestorage.inventory;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.ModConstants;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class EnderOverflowState extends PersistentState {
	private static final int CURRENT_VERSION = 1;
	public SimpleInventory inventory;

	public EnderOverflowState() {
		this.inventory = new SimpleInventory(201);
	}

	public EnderOverflowState(SimpleInventory inventory) {
		this.inventory = inventory;
	}

	public static EnderOverflowState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		int version = nbt.contains("Version") ? nbt.getInt("Version") : 0;

		int size;
		NbtList items;

		if (version == 0) {
			size = nbt.getInt("Size");
			items = nbt.contains("Items") ? nbt.getList("Items", 10) : new NbtList();
			Gestorage.LOGGER.info("Loaded legacy overflow data (version 0), {} slots, {} items", size, items.size());
		} else {
			size = nbt.getInt("Size");
			items = nbt.contains("Items") ? nbt.getList("Items", 10) : new NbtList();
		}

		if (size <= 0) {
			size = 201;
			Gestorage.LOGGER.warn("Invalid overflow size {}, using default 201", nbt.getInt("Size"));
		}

		if (size > ModConstants.EXTRA_LARGE_ENDER_SIZE) {
			size = ModConstants.EXTRA_LARGE_ENDER_SIZE;
			Gestorage.LOGGER.warn("Overflow size {} exceeds maximum {}, clamping", size, ModConstants.EXTRA_LARGE_ENDER_SIZE);
		}

		SimpleInventory inv = new SimpleInventory(size);
		int loadedCount = 0;
		for (int i = 0; i < items.size(); i++) {
			NbtCompound itemNbt = items.getCompound(i);
			int slot = itemNbt.getInt("Slot");
			if (slot < 0 || slot >= size) {
				Gestorage.LOGGER.warn("Invalid slot index {} (size={}), skipping", slot, size);
				continue;
			}
			ItemStack stack = ItemStack.fromNbtOrEmpty(lookup, itemNbt);
			if (!stack.isEmpty()) {
				inv.setStack(slot, stack);
				loadedCount++;
			}
		}
		Gestorage.LOGGER.info("Loaded {}/{} items from overflow (version {})", loadedCount, items.size(), version);

		return new EnderOverflowState(inv);
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		nbt.putInt("Version", CURRENT_VERSION);
		nbt.putInt("Size", inventory.size());
		NbtList items = new NbtList();
		int savedCount = 0;
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (!stack.isEmpty()) {
				NbtCompound itemNbt = (NbtCompound) stack.encodeAllowEmpty(lookup);
				itemNbt.putInt("Slot", i);
				items.add(itemNbt);
				savedCount++;
			}
		}
		nbt.put("Items", items);
		Gestorage.LOGGER.debug("Saved {} items to overflow (version {})", savedCount, CURRENT_VERSION);
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

	public static void createBackup(Path worldDir, UUID playerUuid) {
		try {
			Path dataDir = worldDir.resolve("data");
			Files.createDirectories(dataDir);
			String key = getKey(playerUuid);
			Path source = dataDir.resolve(key + ".dat");
			if (Files.exists(source)) {
				Path backup = dataDir.resolve(key + ".dat.backup_v" + CURRENT_VERSION);
				Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
				Gestorage.LOGGER.info("Created backup: {}", backup.getFileName());
			}
		} catch (IOException e) {
			Gestorage.LOGGER.error("Failed to create backup for player {}", playerUuid, e);
		}
	}
}
