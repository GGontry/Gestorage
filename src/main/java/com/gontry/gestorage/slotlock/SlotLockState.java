package com.gontry.gestorage.slotlock;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.ModConstants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * Per-player, per-world set of locked slots, split by scope.
 *
 * <p>Locks are stored by slot index, not by item: moving an item out of a locked
 * slot and back keeps that slot locked. The state is versioned and every read is
 * defensive so a corrupt or newer file resets to "no locks" instead of throwing.
 */
public class SlotLockState extends PersistentState {
	private static final int CURRENT_VERSION = 1;
	private static final long FLUSH_INTERVAL_MS = 2000L;
	private static final Map<UUID, SlotLockState> CACHE = new ConcurrentHashMap<>();
	/** Int-array NBT codec (same wire format as the old {@code putIntArray}). */
	private static final Codec<int[]> INT_ARRAY = Codec.INT_STREAM.xmap(IntStream::toArray, IntStream::of);

	private final Set<Integer> playerSlots = new TreeSet<>();
	private final Set<Integer> enderSlots = new TreeSet<>();

	private PersistentStateManager manager;
	private long lastFlushMs = 0L;

	public static String getKey(UUID playerUuid) {
		return "gestorage_slot_lock_" + playerUuid;
	}

	/**
	 * Mirrors the NBT layout written before the switch to codecs: a version gate
	 * plus the two int arrays, with every slot range-checked in {@link #fromStored}.
	 */
	public static final Codec<SlotLockState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("Version", 0).forGetter(state -> CURRENT_VERSION),
			INT_ARRAY.optionalFieldOf("PlayerSlots", new int[0]).forGetter(SlotLockState::playerSlotsSnapshot),
			INT_ARRAY.optionalFieldOf("EnderSlots", new int[0]).forGetter(SlotLockState::enderSlotsSnapshot)
	).apply(instance, SlotLockState::fromStored));

	/** State type for this player; identity is the save key, so fresh instances match loaded ones. */
	private static PersistentStateType<SlotLockState> createType(UUID playerUuid) {
		return new PersistentStateType<>(getKey(playerUuid), SlotLockState::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
	}

	public static SlotLockState get(ServerPlayerEntity player) {
		UUID uuid = player.getUuid();
		SlotLockState state = CACHE.get(uuid);
		if (state == null) {
			PersistentStateManager sm = stateManager(player);
			if (sm == null) return new SlotLockState();
			state = sm.getOrCreate(createType(uuid));
			state.manager = sm;
			CACHE.put(uuid, state);
		}
		return state;
	}

	/**
	 * Like {@link #get} but never creates a state file. Sorting and enforcement
	 * checks use this on every tick/click, so players without locks do not end up
	 * with an empty file just for opening their inventory.
	 */
	public static SlotLockState getExisting(ServerPlayerEntity player) {
		UUID uuid = player.getUuid();
		SlotLockState state = CACHE.get(uuid);
		if (state != null) return state;
		PersistentStateManager sm = stateManager(player);
		if (sm == null) return null;
		state = sm.get(createType(uuid));
		if (state != null) {
			state.manager = sm;
			CACHE.put(uuid, state);
		}
		return state;
	}

	private static PersistentStateManager stateManager(ServerPlayerEntity player) {
		MinecraftServer server = player.getEntityWorld().getServer();
		if (server == null) return null;
		return server.getOverworld().getPersistentStateManager();
	}

	public boolean isPlayerLocked(int slot) {
		return playerSlots.contains(slot);
	}

	public boolean isEnderLocked(int slot) {
		return enderSlots.contains(slot);
	}

	public Set<Integer> playerSlotsView() {
		return Collections.unmodifiableSet(playerSlots);
	}

	public Set<Integer> enderSlotsView() {
		return Collections.unmodifiableSet(enderSlots);
	}

	public int[] playerSlotsSnapshot() {
		return playerSlots.stream().mapToInt(Integer::intValue).toArray();
	}

	public int[] enderSlotsSnapshot() {
		return enderSlots.stream().mapToInt(Integer::intValue).toArray();
	}

	public void togglePlayer(int slot) {
		if (slot < 0 || slot >= ModConstants.PLAYER_INVENTORY_SIZE) return;
		toggle(playerSlots, slot);
	}

	public void toggleEnder(int slot) {
		if (slot < 0 || slot >= ModConstants.EXTRA_LARGE_ENDER_SIZE) return;
		toggle(enderSlots, slot);
	}

	private void toggle(Set<Integer> slots, int slot) {
		if (!slots.remove(slot)) {
			slots.add(slot);
		}
		scheduleSave();
	}

	public void scheduleSave() {
		markDirty();
		if (manager == null) return;
		long now = System.currentTimeMillis();
		if (now - lastFlushMs >= FLUSH_INTERVAL_MS) {
			lastFlushMs = now;
			manager.save();
		}
	}

	private static SlotLockState fromStored(int version, int[] playerSlots, int[] enderSlots) {
		if (version != CURRENT_VERSION) {
			Gestorage.LOGGER.warn("[SlotLock] Unknown state version {} (expected {}), starting fresh",
					version, CURRENT_VERSION);
			return new SlotLockState();
		}

		SlotLockState state = new SlotLockState();
		int skipped = 0;

		for (int slot : playerSlots) {
			if (slot >= 0 && slot < ModConstants.PLAYER_INVENTORY_SIZE) {
				state.playerSlots.add(slot);
			} else {
				skipped++;
			}
		}
		for (int slot : enderSlots) {
			if (slot >= 0 && slot < ModConstants.EXTRA_LARGE_ENDER_SIZE) {
				state.enderSlots.add(slot);
			} else {
				skipped++;
			}
		}
		if (skipped > 0) {
			Gestorage.LOGGER.warn("[SlotLock] Ignored {} out-of-range locked slots", skipped);
		}
		return state;
	}

	public static void clearCache(UUID playerUuid) {
		CACHE.remove(playerUuid);
	}

	public static void clearCacheAll() {
		CACHE.clear();
	}
}
