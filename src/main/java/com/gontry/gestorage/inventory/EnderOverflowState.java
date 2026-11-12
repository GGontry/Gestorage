package com.gontry.gestorage.inventory;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.ModConstants;
import com.gontry.gestorage.StackCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
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

	/**
	 * One stored overflow entry. A record codec merges its fields with the item's
	 * own, so the persisted shape stays the flat {@code {Slot: ..., id: ...}}
	 * layout this state wrote before the switch to codecs.
	 */
	private record StoredItem(int slot, ItemStack stack) {
		public static final Codec<StoredItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("Slot").forGetter(StoredItem::slot),
				StackCodecs.TOLERANT.forGetter(StoredItem::stack)
		).apply(instance, StoredItem::new));
	}

	/**
	 * Mirrors the NBT layout written before the switch to codecs, including the
	 * legacy version-0 files (which logged and loaded the same fields).
	 */
	public static final Codec<EnderOverflowState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("Version", 0).forGetter(state -> CURRENT_VERSION),
			Codec.INT.optionalFieldOf("Size", 0).forGetter(state -> state.inventory.size()),
			StoredItem.CODEC.listOf().optionalFieldOf("Items", List.of()).forGetter(EnderOverflowState::storedItems)
	).apply(instance, EnderOverflowState::fromStored));

	private static List<StoredItem> storedItems(EnderOverflowState state) {
		List<StoredItem> items = new ArrayList<>(state.inventory.size());
		for (int i = 0; i < state.inventory.size(); i++) {
			ItemStack stack = state.inventory.getStack(i);
			if (!stack.isEmpty()) {
				items.add(new StoredItem(i, stack));
			}
		}
		return items;
	}

	private static EnderOverflowState fromStored(int version, int size, List<StoredItem> items) {
		if (version < 0 || version > CURRENT_VERSION) {
			Gestorage.LOGGER.warn("[Overflow] Unknown state version {} (expected {}), starting fresh", version, CURRENT_VERSION);
			return new EnderOverflowState();
		}
		if (version == 0) {
			Gestorage.LOGGER.info("[Overflow] Loaded legacy data (version 0), size={}, entries={}", size, items.size());
		}

		if (size <= 0) {
			Gestorage.LOGGER.warn("[Overflow] Invalid size {} in stored data, defaulting to 201", size);
			size = 201;
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
		for (StoredItem item : items) {
			if (item.slot() < 0 || item.slot() >= size) {
				Gestorage.LOGGER.warn("[Overflow] Slot index {} out of range [0,{}), skipping item", item.slot(), size);
				skippedCount++;
				continue;
			}
			if (!item.stack().isEmpty()) {
				inv.setStack(item.slot(), item.stack());
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

	public static String getKey(UUID playerUuid) {
		return "gestorage_ender_overflow_" + playerUuid.toString();
	}

	/** State type for this player; identity is the save key, so fresh instances match loaded ones. */
	private static PersistentStateType<EnderOverflowState> createType(UUID playerUuid) {
		return new PersistentStateType<>(getKey(playerUuid), EnderOverflowState::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
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
		return manager.getOrCreate(createType(playerUuid));
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
