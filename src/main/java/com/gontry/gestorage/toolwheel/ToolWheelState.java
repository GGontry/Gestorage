package com.gontry.gestorage.toolwheel;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.ModConstants;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

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
	public ItemStack defaultTool = ItemStack.EMPTY;
	public int defaultSlot = ModConstants.TOOL_SLOT_NONE;

	private PersistentStateManager manager;
	private long lastFlushMs = 0L;

	public static String getKey(UUID playerUuid) {
		return "gestorage_tool_wheel_" + playerUuid.toString();
	}

	public static ToolWheelState get(ServerPlayerEntity player) {
		UUID uuid = player.getUuid();
		ToolWheelState state = CACHE.get(uuid);
		if (state == null) {
			PersistentStateManager sm = player.getServer().getOverworld().getPersistentStateManager();
			state = sm.getOrCreate(
					new PersistentState.Type<>(ToolWheelState::new, ToolWheelState::fromNbt, null),
					getKey(uuid)
			);
			state.manager = sm;
			CACHE.put(uuid, state);
		}
		return state;
	}

	public static ToolWheelState getExisting(ServerPlayerEntity player) {
		UUID uuid = player.getUuid();
		ToolWheelState state = CACHE.get(uuid);
		if (state != null) return state;
		PersistentStateManager sm = player.getServer().getOverworld().getPersistentStateManager();
		state = sm.get(
				new PersistentState.Type<>(ToolWheelState::new, ToolWheelState::fromNbt, null),
				getKey(uuid)
		);
		if (state != null) {
			state.manager = sm;
			CACHE.put(uuid, state);
		}
		return state;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		nbt.putInt("Version", CURRENT_VERSION);
		nbt.putInt("Size", SIZE);
		nbt.putBoolean("AutoTool", autoTool);
		nbt.put("DefaultTool", defaultTool.encodeAllowEmpty(lookup));
		nbt.putInt("DefaultSlot", defaultSlot);
		NbtList items = new NbtList();
		for (int i = 0; i < SIZE; i++) {
			ItemStack stack = stacks.get(i);
			if (!stack.isEmpty()) {
				NbtCompound itemNbt = (NbtCompound) stack.encodeAllowEmpty(lookup);
				itemNbt.putInt("Slot", i);
				items.add(itemNbt);
			}
		}
		nbt.put("Items", items);
		return nbt;
	}

	public static ToolWheelState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		int version = nbt.contains("Version") ? nbt.getInt("Version") : 0;
		if (version != CURRENT_VERSION) {
			Gestorage.LOGGER.warn("[ToolWheel] Unknown state version {} (expected {}), starting fresh", version, CURRENT_VERSION);
			return new ToolWheelState();
		}
		ToolWheelState state = new ToolWheelState();
		state.autoTool = nbt.getBoolean("AutoTool");
		if (nbt.contains("DefaultTool", NbtElement.COMPOUND_TYPE)) {
			ItemStack defaultTool = ItemStack.fromNbtOrEmpty(lookup, nbt.getCompound("DefaultTool"));
			state.defaultTool = defaultTool;
		}
		if (nbt.contains("DefaultSlot", NbtElement.INT_TYPE)) {
			int slot = nbt.getInt("DefaultSlot");
			if (PlayerInventory.isValidHotbarIndex(slot)) {
				state.defaultSlot = slot;
			} else {
				Gestorage.LOGGER.warn("[ToolWheel] Ignored out-of-range default slot {}", slot);
			}
		}
		NbtList items = nbt.contains("Items") ? nbt.getList("Items", 10) : new NbtList();
		int loaded = 0;
		int skipped = 0;
		for (int i = 0; i < items.size(); i++) {
			NbtCompound itemNbt = items.getCompound(i);
			int slot = itemNbt.getInt("Slot");
			if (slot < 0 || slot >= SIZE) {
				skipped++;
				continue;
			}
			ItemStack stack = ItemStack.fromNbtOrEmpty(lookup, itemNbt);
			if (!stack.isEmpty()) {
				state.stacks.set(slot, stack);
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