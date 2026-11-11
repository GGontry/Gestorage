package com.gontry.gestorage.toolwheel;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.ModConstants;
import com.gontry.gestorage.StackCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ToolWheelState extends PersistentState {
	public static final int SIZE = 9;
	private static final int CURRENT_VERSION = 1;
	private static final long FLUSH_INTERVAL_MS = 2000L;
	private static final Map<UUID, ToolWheelState> CACHE = new ConcurrentHashMap<>();

	public final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
	public boolean autoTool = false;
	public int defaultSlot = ModConstants.TOOL_SLOT_NONE;
	public int enchPref = ModConstants.ENCH_PREF_NONE;

	/**
	 * One stored wheel entry. A record codec merges its fields with the item's own,
	 * so the persisted shape stays the flat {@code {Slot: ..., id: ..., count: ...}}
	 * layout this state wrote before the switch to codecs.
	 */
	private record WheelItem(int slot, ItemStack stack) {
		public static final Codec<WheelItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("Slot").forGetter(WheelItem::slot),
				StackCodecs.TOLERANT.forGetter(WheelItem::stack)
		).apply(instance, WheelItem::new));
	}

	/**
	 * Mirrors the NBT layout written before the switch to codecs, including the
	 * purely informational {@code Size} field. Everything but the version is
	 * optional so a partially readable file keeps the defaults instead of failing
	 * the whole state, and every value is range-checked in {@link #fromStored}.
	 */
	public static final Codec<ToolWheelState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("Version", 0).forGetter(state -> CURRENT_VERSION),
			Codec.INT.optionalFieldOf("Size", 0).forGetter(state -> SIZE),
			Codec.BOOL.optionalFieldOf("AutoTool", false).forGetter(state -> state.autoTool),
			Codec.INT.optionalFieldOf("DefaultSlot", ModConstants.TOOL_SLOT_NONE).forGetter(state -> state.defaultSlot),
			Codec.INT.optionalFieldOf("EnchPref", ModConstants.ENCH_PREF_NONE).forGetter(state -> state.enchPref),
			WheelItem.CODEC.listOf().optionalFieldOf("Items", List.of()).forGetter(ToolWheelState::storedItems)
	).apply(instance, ToolWheelState::fromStored));

	private PersistentStateManager manager;
	private long lastFlushMs = 0L;

	public static String getKey(UUID playerUuid) {
		return "gestorage_tool_wheel_" + playerUuid.toString();
	}

	/** State type for this player; identity is the save key, so fresh instances match loaded ones. */
	private static PersistentStateType<ToolWheelState> createType(UUID playerUuid) {
		return new PersistentStateType<>(getKey(playerUuid), ToolWheelState::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
	}

	public static ToolWheelState get(ServerPlayerEntity player) {
		UUID uuid = player.getUuid();
		ToolWheelState state = CACHE.get(uuid);
		if (state == null) {
			PersistentStateManager sm = player.getEntityWorld().getServer().getOverworld().getPersistentStateManager();
			state = sm.getOrCreate(createType(uuid));
			state.manager = sm;
			CACHE.put(uuid, state);
		}
		return state;
	}

	public static ToolWheelState getExisting(ServerPlayerEntity player) {
		UUID uuid = player.getUuid();
		ToolWheelState state = CACHE.get(uuid);
		if (state != null) return state;
		PersistentStateManager sm = player.getEntityWorld().getServer().getOverworld().getPersistentStateManager();
		state = sm.get(createType(uuid));
		if (state != null) {
			state.manager = sm;
			CACHE.put(uuid, state);
		}
		return state;
	}

	/**
	 * Returns the player's stored wheel without ever creating one, so callers such as
	 * the join sync can tell "no wheel data" from "an empty wheel".
	 *
	 * <p>{@link PersistentStateManager#get} also returns a state that is already in
	 * memory but has not been written to disk yet (a dirty state survives a quick
	 * disconnect/reconnect), which a file existence check would miss.
	 */
	public static ToolWheelState getStored(ServerPlayerEntity player) {
		return getExisting(player);
	}

	private static List<WheelItem> storedItems(ToolWheelState state) {
		List<WheelItem> items = new ArrayList<>(SIZE);
		for (int i = 0; i < SIZE; i++) {
			ItemStack stack = state.stacks.get(i);
			if (!stack.isEmpty()) {
				items.add(new WheelItem(i, stack));
			}
		}
		return items;
	}

	private static ToolWheelState fromStored(int version, int size, boolean autoTool, int defaultSlot, int enchPref,
			List<WheelItem> items) {
		if (version != CURRENT_VERSION) {
			Gestorage.LOGGER.warn("[ToolWheel] Unknown state version {} (expected {}), starting fresh", version, CURRENT_VERSION);
			return new ToolWheelState();
		}
		ToolWheelState state = new ToolWheelState();
		state.autoTool = autoTool;
		if (defaultSlot != ModConstants.TOOL_SLOT_NONE) {
			if (PlayerInventory.isValidHotbarIndex(defaultSlot)) {
				state.defaultSlot = defaultSlot;
			} else {
				Gestorage.LOGGER.warn("[ToolWheel] Ignored out-of-range default slot {}", defaultSlot);
			}
		}
		if (enchPref >= ModConstants.ENCH_PREF_NONE && enchPref <= ModConstants.ENCH_PREF_MAX) {
			state.enchPref = enchPref;
		} else {
			Gestorage.LOGGER.warn("[ToolWheel] Ignored out-of-range enchantment preference {}", enchPref);
		}
		int loaded = 0;
		int skipped = 0;
		for (WheelItem item : items) {
			if (item.slot() < 0 || item.slot() >= SIZE) {
				skipped++;
				continue;
			}
			if (!item.stack().isEmpty()) {
				state.stacks.set(item.slot(), item.stack());
				loaded++;
			}
		}
		if (skipped > 0) {
			Gestorage.LOGGER.warn("[ToolWheel] Skipped {} items with invalid slots (loaded {}/{})", skipped, loaded, items.size());
		}
		return state;
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

	public void forceSave() {
		markDirty();
		if (manager != null) manager.save();
	}

	public static void clearCache(UUID playerUuid) {
		CACHE.remove(playerUuid);
	}

	public static void clearCacheAll() {
		CACHE.clear();
	}
}
