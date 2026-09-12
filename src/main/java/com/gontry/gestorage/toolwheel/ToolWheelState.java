package com.gontry.gestorage.toolwheel;

import com.gontry.gestorage.Gestorage;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
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

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		nbt.putInt("Version", CURRENT_VERSION);
		nbt.putInt("Size", SIZE);
		nbt.putBoolean("AutoTool", autoTool);
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
		ToolWheelState state = new ToolWheelState();
		state.autoTool = nbt.getBoolean("AutoTool");
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