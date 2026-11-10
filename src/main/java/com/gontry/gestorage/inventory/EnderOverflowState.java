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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EnderOverflowState extends PersistentState {
	private static final int CURRENT_VERSION = 1;
	/** Minimum time between two world saves triggered by the same player's overflow. */
	private static final long FLUSH_INTERVAL_MS = 2000L;
	private static final Set<UUID> backedUpThisSession = ConcurrentHashMap.newKeySet();
	public SimpleInventory inventory;
	/**
	 * Wall clock of the last flush, shared by every wrapper of this state. The
	 * throttle lives here (not in the menus) so short-lived wrappers created by
	 * the refill path cannot force a world save on every request.
	 */
	private long lastFlushMs = 0L;

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
			Gestorage.LOGGER.info("[Overflow] Loaded legacy data (version 0), size={}, entries={}", size, items.size());
		} else {
			size = nbt.getInt("Size");
			items = nbt.contains("Items") ? nbt.getList("Items", 10) : new NbtList();
		}

		if (size <= 0) {
			size = 201;
			Gestorage.LOGGER.warn("[Overflow] Invalid size {} in NBT, defaulting to 201", nbt.getInt("Size"));
		}

		if (size > ModConstants.EXTRA_LARGE_ENDER_SIZE) {
			Gestorage.LOGGER.warn("[Overflow] Size {} exceeds max {}, clamping", size, ModConstants.EXTRA_LARGE_ENDER_SIZE);
			size = ModConstants.EXTRA_LARGE_ENDER_SIZE;
		}

		int expectedOverflow = ModConstants.EXTRA_LARGE_ENDER_SIZE - ModConstants.VANILLA_ENDER_SIZE;
		if (size != expectedOverflow) {
			Gestorage.LOGGER.warn("[Overflow] Size {} differs from expected overflow size {} — possible version mismatch", size, expectedOverflow);
		}

		SimpleInventory inv = new SimpleInventory(size);
		int loadedCount = 0;
		int skippedCount = 0;
		for (int i = 0; i < items.size(); i++) {
			NbtCompound itemNbt = items.getCompound(i);
			int slot = itemNbt.getInt("Slot");
			if (slot < 0 || slot >= size) {
				Gestorage.LOGGER.warn("[Overflow] Slot index {} out of range [0,{}), skipping item", slot, size);
				skippedCount++;
				continue;
			}
			ItemStack stack = ItemStack.fromNbtOrEmpty(lookup, itemNbt);
			if (!stack.isEmpty()) {
				inv.setStack(slot, stack);
				loadedCount++;
			}
		}
		if (skippedCount > 0) {
			Gestorage.LOGGER.warn("[Overflow] Skipped {} items due to invalid slots (loaded {}/{})", skippedCount, loadedCount, items.size());
		} else {
			Gestorage.LOGGER.info("[Overflow] Loaded {}/{} items (version {})", loadedCount, items.size(), version);
		}

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
		return nbt;
	}

	public static String getKey(UUID playerUuid) {
		return "gestorage_ender_overflow_" + playerUuid.toString();
	}

	public void flush(PersistentStateManager manager) {
		if (manager == null || !isDirty()) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - lastFlushMs < FLUSH_INTERVAL_MS) {
			return;
		}
		lastFlushMs = now;
		manager.save();
	}

	/** Saves right away, bypassing the throttle (used when a menu closes). */
	public void flushNow(PersistentStateManager manager) {
		if (manager == null || !isDirty()) {
			return;
		}
		lastFlushMs = System.currentTimeMillis();
		manager.save();
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

	public static void resetSessionBackups() {
		backedUpThisSession.clear();
	}

	/**
	 * Copies the stored overflow NBT once per server session, before the state can
	 * be written again. Called from the ender chest factory, which is the only
	 * construction point for the enhanced inventory, so every path that can touch
	 * the overflow is covered.
	 */
	public static void sessionBackup(Path worldDir, UUID playerUuid) {
		if (!backedUpThisSession.add(playerUuid)) {
			return;
		}
		try {
			Path dataDir = worldDir.resolve("data");
			Files.createDirectories(dataDir);
			String key = getKey(playerUuid);
			Path source = dataDir.resolve(key + ".dat");
			if (Files.exists(source)) {
				Path backup = dataDir.resolve(key + ".dat.session_backup");
				Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
				Gestorage.LOGGER.info("[Overflow] Session backup created for {}: {}", playerUuid, backup.getFileName());
			}
		} catch (IOException e) {
			Gestorage.LOGGER.warn("[Overflow] Could not create session backup for {}: {}", playerUuid, e.getMessage());
		}
	}
}
