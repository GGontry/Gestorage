package com.gontry.gestorage.toolwheel;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.ModConstants;
import com.gontry.gestorage.network.ToolWheelSyncS2CPacket;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Auto Tool session bookkeeping. A session keeps the rotation that is currently
 * applied to the player's tool slot so it can always be undone, whatever happens to
 * the Auto Tool flag in the meantime.
 *
 * <p>Every swap is a real inventory rotation: the tool slot and the wheel slot
 * trade their stacks. Because of that a second swap started before the first one
 * was undone would leave both tools in the wrong place, so a pending swap is always
 * reverted before another one begins. A revert only ever swaps those two slots back,
 * never moves an item to a slot the rotation did not involve and never changes the
 * slot the player has selected.
 */
public final class AutoToolManager {
	private static final long REVERT_DELAY_MS = 1000L;
	/** Ticks between two re-evaluations of the swap while a block is being mined. */
	public static final int AUTO_EVAL_INTERVAL_TICKS = 4;
	/**
	 * Vanilla's floor for a mining tool: {@code Item.getMiningSpeed} returns 1.0
	 * for anything that is not effective on the block (a bare hand, a sword, a
	 * block, a stick). A wheel stack only counts as a candidate above it, so a
	 * non-tool is never swapped in and the Efficiency bonus is only granted to a
	 * tool that actually breaks the block.
	 */
	private static final float MIN_EFFECTIVE_SPEED = 1.0F;
	/** Sentinel telling the plain speed pass has to decide because no preferred tool can mine the block. */
	private static final int PREFERENCE_NOT_APPLICABLE = -2;
	private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
	/** Players whose client reported the Tool Wheel module as switched off. */
	private static final Set<UUID> MODULE_OFF = ConcurrentHashMap.newKeySet();

	private AutoToolManager() {}

	/**
	 * Called whenever the Auto Tool flag flips. Enabling it opens a session for the
	 * swaps of this activation, disabling it reverts a pending swap right away so a
	 * tool swapped in mid-vein never stays in the player's hand.
	 */
	public static void onAutoToolToggled(ServerPlayerEntity player, boolean enabled) {
		UUID id = player.getUuid();
		Session session = SESSIONS.get(id);
		if (enabled) {
			if (session != null) {
				if (session.hasPending()) revert(player, session);
				SESSIONS.remove(id);
			}
			SESSIONS.put(id, new Session());
			return;
		}
		if (session == null) return;
		if (session.hasPending()) revert(player, session);
		SESSIONS.remove(id);
	}

	/**
	 * Re-evaluates the best wheel tool for {@code pos} and swaps it into the tool slot
	 * when it beats what the hand holds. Called both on {@code START_DESTROY_BLOCK} and
	 * every {@link #AUTO_EVAL_INTERVAL_TICKS} ticks while the block is really being
	 * broken: the client can send the action for a position the server already
	 * considers gone (it keeps the previous target for a tick), which used to make the
	 * swap silently miss and only succeed on a later click.
	 */
	public static void onMineStart(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		try {
			evaluate(player, world, pos);
		} catch (RuntimeException e) {
			Gestorage.LOGGER.error("Auto Tool could not pick a tool for {} while mining {}",
					player.getName().getString(), pos.toShortString(), e);
		}
	}

	/**
	 * The module flag lives in the client config, so the server only learns about it
	 * when the client reports it (on join and on every toggle). A player counts as
	 * enabled until the client says otherwise, and reporting "off" undoes an active
	 * swap right away so no tool is left in the hand of a disabled module.
	 */
	public static void setModuleEnabled(ServerPlayerEntity player, boolean enabled) {
		UUID id = player.getUuid();
		if (enabled) {
			MODULE_OFF.remove(id);
			return;
		}
		MODULE_OFF.add(id);
		Session session = SESSIONS.get(id);
		if (session == null) return;
		if (session.hasPending()) revert(player, session);
		SESSIONS.remove(id);
	}

	/** Whether the client still has the Tool Wheel module switched on. */
	public static boolean isModuleEnabled(ServerPlayerEntity player) {
		return !MODULE_OFF.contains(player.getUuid());
	}

	private static void evaluate(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {		if (player.isCreative() || player.isSpectator()) return;
		UUID id = player.getUuid();
		Session session = SESSIONS.get(id);
		ToolWheelState state = ToolWheelState.getExisting(player);
		if (state == null || !state.autoTool || MODULE_OFF.contains(id)) {
			holdPending(player, session);
			return;
		}

		PlayerInventory inv = player.getInventory();
		int handSlot = ToolWheelLogic.resolveToolSlot(state, inv);
		if (session == null) {
			session = new Session();
			SESSIONS.put(id, session);
		}

		BlockState target = world.getBlockState(pos);
		if (target.isAir()) {
			holdPending(player, session);
			return;
		}

		int pref = state.enchPref;
		Enchants enchants = enchants(player, pref);
		int wheelSlot = chooseSlot(state, target, inv.getStack(handSlot), pref, enchants);
		if (wheelSlot < 0) {
			// The hand already holds the best tool, but the player may be on another
			// hotbar slot: Auto Tool always moves to the resolved tool slot (the pinned
			// Default Slot) so it is actually used.
			selectSlot(player, handSlot);
			holdPending(player, session);
			return;
		}

		if (session.hasPending()) {
			// Two swaps in a row without undoing the first one would leave the auto tool
			// and whatever the tool slot held in each other's place, so roll the previous
			// swap back first and decide again on the restored layout.
			revert(player, session);
			wheelSlot = chooseSlot(state, target, inv.getStack(handSlot), pref, enchants);
			if (wheelSlot < 0) {
				selectSlot(player, handSlot);
				return;
			}
		}

		ItemStack displaced = inv.getStack(handSlot);
		applySwap(player, state, inv, handSlot, wheelSlot);
		session.beginPending(wheelSlot, handSlot, inv.getStack(handSlot).getItem(), displaced);
	}

	/**
	 * Keeps an already swapped tool in place for another mining cycle, or drops the
	 * pending record once the hand no longer holds the tool Auto Tool put there (the
	 * player moved it, so nothing may be forced back).
	 */
	private static void holdPending(ServerPlayerEntity player, Session session) {
		if (session == null) return;
		if (session.hasPending() && player.isAlive() && handHoldsTool(player, session)) {
			session.refresh();
		} else {
			session.clearPending();
		}
	}

	public static void refresh(ServerPlayerEntity player) {
		Session session = SESSIONS.get(player.getUuid());
		if (session != null) session.lastMineTime = System.currentTimeMillis();
	}

	public static void tick(ServerPlayerEntity player) {
		UUID id = player.getUuid();
		Session session = SESSIONS.get(id);
		if (session == null) return;
		if (!player.isAlive()) {
			SESSIONS.remove(id);
			return;
		}
		if (!session.hasPending()) return;
		if (System.currentTimeMillis() - session.lastMineTime >= REVERT_DELAY_MS) {
			revert(player, session);
		}
	}

	/**
	 * Undoes the last rotation: the stack Auto Tool displaced goes back into the tool
	 * slot and the auto tool goes back into the wheel slot it came from. The selected
	 * hotbar slot is never touched, so scrolling away from the tool slot while Auto Tool
	 * works is never fought over, and no item is ever moved between slots the swap did
	 * not involve: when either stack has been moved by the player the layout already is
	 * what the player wants and only the pending record is dropped.
	 */
	private static void revert(ServerPlayerEntity player, Session session) {
		int wheelSlot = session.wheelSlot;
		int handSlot = session.handSlot;
		Item toolItem = session.toolItem;
		ItemStack displaced = session.displaced;
		session.clearPending();
		ToolWheelState state = ToolWheelState.getExisting(player);
		if (state == null || handSlot < 0 || handSlot >= PlayerInventory.MAIN_SIZE) return;
		if (wheelSlot < 0 || wheelSlot >= ToolWheelState.SIZE) return;
		PlayerInventory inv = player.getInventory();
		ItemStack toolNow = inv.getStack(handSlot);
		if (toolNow.isEmpty() || !toolNow.isOf(toolItem)) return;
		ItemStack parked = state.stacks.get(wheelSlot);
		if (!holdsDisplaced(parked, displaced)) return;
		inv.setStack(handSlot, parked);
		state.stacks.set(wheelSlot, toolNow);
		inv.markDirty();
		state.scheduleSave();
		ToolWheelSyncS2CPacket.sendTo(player);
	}

	/**
	 * Whether the wheel slot Auto Tool used still holds the stack it displaced. Matching
	 * runs from strictest to loosest (identical stack, same item and components, same
	 * item) so a stack that was used, repaired or re-enchanted while parked is still
	 * recognised, and an empty displacement only matches an empty slot.
	 */
	private static boolean holdsDisplaced(ItemStack parked, ItemStack displaced) {
		if (displaced.isEmpty()) return parked.isEmpty();
		for (int level = 0; level < 3; level++) {
			if (matches(parked, displaced, level)) return true;
		}
		return false;
	}

	private static boolean matches(ItemStack stack, ItemStack target, int level) {
		if (stack.isEmpty()) return false;
		return switch (level) {
			case 0 -> ItemStack.areEqual(stack, target);
			case 1 -> ItemStack.areItemsAndComponentsEqual(stack, target);
			default -> stack.getItem() == target.getItem();
		};
	}

	/**
	 * Rotates the tool slot and one wheel slot, then tells the owning client about it.
	 */
	private static void applySwap(ServerPlayerEntity player, ToolWheelState state, PlayerInventory inv,
			int handSlot, int wheelSlot) {
		ItemStack hand = inv.getStack(handSlot);
		ItemStack tool = state.stacks.get(wheelSlot);
		inv.setStack(handSlot, tool);
		state.stacks.set(wheelSlot, hand);
		inv.markDirty();
		state.scheduleSave();
		selectSlot(player, handSlot);
		ToolWheelSyncS2CPacket.sendTo(player);
	}

	/**
	 * Index of the wheel stack Auto Tool must use for the target block, or {@code -1}
	 * when the hand already holds the best tool.
	 *
	 * <p>With an enchantment preference set, a wheel tool carrying that enchantment always
	 * wins as long as it can mine the block at all (speed above the non-tool floor) and
	 * beats the preferred tool already in the hand, so a Silk Touch tool is used even
	 * against a faster Fortune one. When no preferred tool can mine the block, Auto Tool
	 * falls back to plain speed: the fastest tool, and nothing at all when the hand is
	 * already the fastest.
	 */
	private static int chooseSlot(ToolWheelState state, BlockState target, ItemStack hand, int pref,
			Enchants enchants) {
		int bestSlot = PREFERENCE_NOT_APPLICABLE;
		if (pref != ModConstants.ENCH_PREF_NONE) {
			float prefSpeed = hasPreferredEnchant(pref, hand, enchants)
					? miningSpeed(hand, target, enchants.efficiency()) : -1.0F;
			if (prefSpeed > MIN_EFFECTIVE_SPEED) bestSlot = -1;
			for (int i = 0; i < ToolWheelState.SIZE; i++) {
				ItemStack stack = state.stacks.get(i);
				if (stack.isEmpty() || !hasPreferredEnchant(pref, stack, enchants)) continue;
				float speed = miningSpeed(stack, target, enchants.efficiency());
				if (speed <= MIN_EFFECTIVE_SPEED) continue;
				if (speed > prefSpeed) {
					prefSpeed = speed;
					bestSlot = i;
				}
			}
		}
		if (bestSlot == PREFERENCE_NOT_APPLICABLE) {
			bestSlot = fastestSlot(state, target, hand, enchants.efficiency());
		}
		return bestSlot;
	}

	/** Index of the fastest wheel tool, or {@code -1} when the hand is already the fastest. */
	private static int fastestSlot(ToolWheelState state, BlockState target, ItemStack hand,
			RegistryEntry<Enchantment> efficiency) {
		float bestSpeed = miningSpeed(hand, target, efficiency);
		int bestSlot = -1;
		for (int i = 0; i < ToolWheelState.SIZE; i++) {
			ItemStack stack = state.stacks.get(i);
			if (stack.isEmpty()) continue;
			float speed = miningSpeed(stack, target, efficiency);
			if (speed > bestSpeed) {
				bestSpeed = speed;
				bestSlot = i;
			}
		}
		return bestSlot;
	}

	private static boolean handHoldsTool(ServerPlayerEntity player, Session session) {
		ItemStack hand = player.getInventory().getStack(session.handSlot);
		return !hand.isEmpty() && hand.isOf(session.toolItem);
	}

	private static Enchants enchants(ServerPlayerEntity player, int pref) {
		DynamicRegistryManager manager = player.getServer() != null ? player.getServer().getRegistryManager() : null;
		Registry<Enchantment> registry = manager != null ? manager.get(RegistryKeys.ENCHANTMENT) : null;
		RegistryEntry<Enchantment> efficiency = registry != null
				? registry.getEntry(Enchantments.EFFICIENCY).orElse(null) : null;
		boolean wanted = pref != ModConstants.ENCH_PREF_NONE && registry != null;
		RegistryEntry<Enchantment> fortune = wanted
				? registry.getEntry(Enchantments.FORTUNE).orElse(null) : null;
		RegistryEntry<Enchantment> silkTouch = wanted
				? registry.getEntry(Enchantments.SILK_TOUCH).orElse(null) : null;
		return new Enchants(efficiency, fortune, silkTouch);
	}

	private static boolean hasPreferredEnchant(int pref, ItemStack stack, Enchants enchants) {
		if (pref == ModConstants.ENCH_PREF_NONE) return false;
		RegistryEntry<Enchantment> wanted = pref == ModConstants.ENCH_PREF_FORTUNE
				? enchants.fortune() : enchants.silkTouch();
		return wanted != null && EnchantmentHelper.getLevel(wanted, stack) > 0;
	}

	/**
	 * Vanilla's mining speed for a stack: the tool speed plus the Efficiency bonus,
	 * which vanilla only grants when the tool is effective on the block. Anything that
	 * cannot break the block scores the bare {@link #MIN_EFFECTIVE_SPEED}.
	 */
	private static float miningSpeed(ItemStack stack, BlockState target, RegistryEntry<Enchantment> efficiency) {
		float speed = stack.getMiningSpeedMultiplier(target);
		if (speed <= MIN_EFFECTIVE_SPEED || efficiency == null) return speed;
		int efficiencyLevel = EnchantmentHelper.getLevel(efficiency, stack);
		if (efficiencyLevel > 0) {
			speed += efficiencyLevel * efficiencyLevel + 1.0F;
		}
		return speed;
	}

	/**
	 * Moves the player's hand to {@code slot} and tells the owning client about it,
	 * otherwise the client would keep rendering and clicking the old slot.
	 */
	private static void selectSlot(ServerPlayerEntity player, int slot) {
		PlayerInventory inv = player.getInventory();
		if (inv.selectedSlot == slot) return;
		inv.selectedSlot = slot;
		if (player.networkHandler != null) {
			player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(slot));
		}
	}

	public static void clear(UUID playerUuid) {
		SESSIONS.remove(playerUuid);
		MODULE_OFF.remove(playerUuid);
	}

	public static void clearAll() {
		SESSIONS.clear();
		MODULE_OFF.clear();
	}

	private record Enchants(RegistryEntry<Enchantment> efficiency, RegistryEntry<Enchantment> fortune,
			RegistryEntry<Enchantment> silkTouch) {}

	private static final class Session {
		volatile long lastMineTime = System.currentTimeMillis();
		int wheelSlot = -1;
		int handSlot = -1;
		Item toolItem = null;
		ItemStack displaced = ItemStack.EMPTY;

		Session() {}

		void refresh() {
			this.lastMineTime = System.currentTimeMillis();
		}

		/**
		 * Records the rotation as it happened: the wheel slot the auto tool came from,
		 * the tool slot it was written to, the auto tool item and the stack it displaced,
		 * which is the one {@link #revert} hands back.
		 */
		void beginPending(int wheelSlot, int handSlot, Item toolItem, ItemStack displaced) {
			this.wheelSlot = wheelSlot;
			this.handSlot = handSlot;
			this.toolItem = toolItem;
			this.displaced = displaced.copy();
			this.lastMineTime = System.currentTimeMillis();
		}

		void clearPending() {
			this.wheelSlot = -1;
			this.handSlot = -1;
			this.toolItem = null;
			this.displaced = ItemStack.EMPTY;
		}

		boolean hasPending() {
			return toolItem != null;
		}
	}
}
