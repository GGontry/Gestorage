package com.gontry.gestorage.slotlock;

import com.gontry.gestorage.ModConstants;
import com.gontry.gestorage.inventory.EnhancedEnderChestInventory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative slot lock rules.
 *
 * <p>The client only mirrors the locked slot set; every decision (sorting,
 * dropping and using) happens here so a modified client cannot bypass it. The
 * module settings are transient: the owning client pushes them when it joins and
 * whenever they change. Locks themselves are persisted by {@link SlotLockState}.
 */
public final class SlotLockManager {
	/**
	 * Client-controlled switches for the module, pushed over the config packet.
	 * They are not persisted server-side because they belong to the client config.
	 */
	public record Settings(boolean enabled, boolean sortingSkipsLockedSlots,
			boolean preventDrop, boolean preventUse) {
		public static final Settings DISABLED = new Settings(false, false, false, false);
	}

	private static final Map<UUID, Settings> SETTINGS = new ConcurrentHashMap<>();

	private SlotLockManager() {}

	public static void setSettings(ServerPlayerEntity player, Settings settings) {
		SETTINGS.put(player.getUuid(), settings);
	}

	public static Settings settings(ServerPlayerEntity player) {
		return SETTINGS.getOrDefault(player.getUuid(), Settings.DISABLED);
	}

	public static void clear(UUID playerUuid) {
		SETTINGS.remove(playerUuid);
	}

	public static void clearAll() {
		SETTINGS.clear();
	}

	public static boolean isValidScope(int scope) {
		return scope == ModConstants.SLOT_SCOPE_PLAYER || scope == ModConstants.SLOT_SCOPE_ENDER;
	}

	/** Whether the player currently has a slot with this scope/index on their open screen. */
	public static boolean hasVisibleSlot(ServerPlayerEntity player, int scope, int slot) {
		if (!isValidScope(scope) || slot < 0) return false;
		if (scope == ModConstants.SLOT_SCOPE_PLAYER && slot >= ModConstants.PLAYER_INVENTORY_SIZE) return false;
		if (scope == ModConstants.SLOT_SCOPE_ENDER && slot >= ModConstants.EXTRA_LARGE_ENDER_SIZE) return false;

		for (Slot candidate : player.currentScreenHandler.slots) {
			if (scopeOf(candidate.inventory) == scope && candidate.getIndex() == slot) {
				return true;
			}
		}
		return false;
	}

	public static void toggle(ServerPlayerEntity player, int scope, int slot) {
		SlotLockState state = SlotLockState.get(player);
		if (scope == ModConstants.SLOT_SCOPE_PLAYER) {
			state.togglePlayer(slot);
		} else if (scope == ModConstants.SLOT_SCOPE_ENDER) {
			state.toggleEnder(slot);
		}
	}

	public static boolean isPlayerSlotLocked(ServerPlayerEntity player, int slot) {
		if (slot < 0 || slot >= ModConstants.PLAYER_INVENTORY_SIZE) return false;
		SlotLockState state = SlotLockState.getExisting(player);
		return state != null && state.isPlayerLocked(slot);
	}

	public static boolean isEnderSlotLocked(ServerPlayerEntity player, int slot) {
		if (slot < 0 || slot >= ModConstants.EXTRA_LARGE_ENDER_SIZE) return false;
		SlotLockState state = SlotLockState.getExisting(player);
		return state != null && state.isEnderLocked(slot);
	}

	public static boolean isLocked(ServerPlayerEntity player, Slot slot) {
		return isLocked(player, scopeOf(slot.inventory), slot.getIndex());
	}

	private static boolean isLocked(ServerPlayerEntity player, int scope, int slot) {
		return switch (scope) {
			case ModConstants.SLOT_SCOPE_PLAYER -> isPlayerSlotLocked(player, slot);
			case ModConstants.SLOT_SCOPE_ENDER -> isEnderSlotLocked(player, slot);
			default -> false;
		};
	}

	private static int scopeOf(Inventory inventory) {
		if (inventory instanceof PlayerInventory) return ModConstants.SLOT_SCOPE_PLAYER;
		if (inventory instanceof EnderChestInventory) return ModConstants.SLOT_SCOPE_ENDER;
		if (inventory instanceof EnhancedEnderChestInventory) return ModConstants.SLOT_SCOPE_ENDER;
		return -1;
	}

	/** Called from {@code ServerPlayerEntity.dropSelectedItem}; Q drops the selected hotbar slot. */
	public static boolean shouldPreventDrop(ServerPlayerEntity player) {
		Settings settings = settings(player);
		if (!settings.enabled() || !settings.preventDrop()) return false;
		return isPlayerSlotLocked(player, player.getInventory().selectedSlot);
	}

	/** Called from the THROW slot action (Q while a screen is open, or dragging an item out). */
	public static boolean shouldPreventDrop(ServerPlayerEntity player, Slot slot) {
		Settings settings = settings(player);
		if (!settings.enabled() || !settings.preventDrop()) return false;
		return isLocked(player, slot);
	}

	/**
	 * Right-click use and block placement for the item in {@code hand}. Blocks are
	 * intentionally not included: opening a chest or a door must keep working even
	 * when the item in hand is locked.
	 */
	public static boolean shouldPreventUse(ServerPlayerEntity player, Hand hand) {
		Settings settings = settings(player);
		if (!settings.enabled() || !settings.preventUse()) return false;
		int slot = hand == Hand.MAIN_HAND
				? player.getInventory().selectedSlot
				: PlayerInventory.OFF_HAND_SLOT;
		return isPlayerSlotLocked(player, slot);
	}

	/**
	 * Slot indices the sorter must leave untouched for the open screen. Returns an
	 * empty set when either the module or the "sorting skips locks" option is off.
	 */
	public static Set<Integer> lockedSlotsForSorting(ServerPlayerEntity player, ScreenHandler screenHandler) {
		Settings settings = settings(player);
		if (!settings.enabled() || !settings.sortingSkipsLockedSlots()) return Set.of();

		SlotLockState state = SlotLockState.getExisting(player);
		if (state == null) return Set.of();

		if (screenHandler instanceof PlayerScreenHandler) {
			return state.playerSlotsView();
		}

		Inventory inventory = screenHandler.slots.isEmpty() ? null : screenHandler.slots.get(0).inventory;
		if (inventory instanceof EnderChestInventory || inventory instanceof EnhancedEnderChestInventory) {
			return state.enderSlotsView();
		}
		return Set.of();
	}
}
