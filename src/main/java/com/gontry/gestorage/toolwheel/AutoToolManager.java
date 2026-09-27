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
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Auto Tool session bookkeeping. A session stores the tool the player was holding
 * when Auto Tool was activated and always returns to it, whatever happens to the
 * Auto Tool flag in the meantime.
 *
 * <p>Every swap is a real inventory rotation: the tool slot and the wheel slot
 * trade their stacks. Because of that a second swap started before the first one
 * was undone would leave both tools in the wrong wheel slot, so a pending swap is
 * always reverted before another one begins.
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
	 * Called whenever the Auto Tool flag flips. Enabling it opens a session around
	 * the tool currently in the tool slot, disabling it reverts a pending swap right
	 * away so a tool swapped in mid-vein never stays in the player's hand.
	 */
	public static void onAutoToolToggled(ServerPlayerEntity player, boolean enabled) {
		UUID id = player.getUuid();
		Session session = SESSIONS.get(id);
		if (enabled) {
			if (session != null) {
				if (session.hasPending()) revert(player, session);
				SESSIONS.remove(id);
			}
			SESSIONS.put(id, newSession(player.getInventory(), ToolWheelState.getExisting(player)));
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
			session = new Session(handSlot, inv.getStack(handSlot).copy());
			SESSIONS.put(id, session);
		} else {
			session.retarget(handSlot, inv);
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
			holdPending(player, session);
			return;
		}

		if (session.hasPending()) {
			// Two swaps in a row without undoing the first one would leave the session
			// tool and the auto tool in each other's wheel slot, so roll the previous
			// swap back first and decide again on the restored layout.
			revert(player, session);
			wheelSlot = chooseSlot(state, target, inv.getStack(handSlot), pref, enchants);
			if (wheelSlot < 0) return;
		}

		applySwap(player, state, inv, handSlot, wheelSlot);
		session.beginPending(wheelSlot, handSlot, inv.getStack(handSlot));
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
	 * Puts the session tool back into the tool slot and the auto tool back where it
	 * came from. The pending swap is always cleared, a manual change in the tool slot
	 * is never clobbered and no item is ever destroyed: when the session tool cannot
	 * be located the auto tool is only parked back in the wheel, and only when that
	 * slot is free, otherwise the hand is left untouched.
	 */
	private static void revert(ServerPlayerEntity player, Session session) {
		int wheelSlot = session.wheelSlot;
		int handSlot = session.handSlot;
		Item toolItem = session.toolItem;
		session.clearPending();
		ToolWheelState state = ToolWheelState.getExisting(player);
		if (state == null) return;
		PlayerInventory inv = player.getInventory();
		ItemStack handNow = inv.getStack(handSlot);
		if (handNow.isEmpty() || !handNow.isOf(toolItem)) return;

		boolean wheelFree = wheelSlot >= 0 && wheelSlot < ToolWheelState.SIZE && state.stacks.get(wheelSlot).isEmpty();
		if (ItemStack.areItemsAndComponentsEqual(handNow, session.returnTool)) {
			if (wheelFree) parkInWheel(inv, state, handSlot, wheelSlot, handNow);
		} else if (session.returnTool.isEmpty()) {
			// The tool slot was empty when Auto Tool was activated, so there is nothing
			// to hand back: parking the auto tool in the wheel is the expected result.
			if (wheelFree) parkInWheel(inv, state, handSlot, wheelSlot, handNow);
		} else {
			int restoreSlot = findRestoreSlot(state, inv, session.returnTool, handSlot, wheelSlot);
			if (restoreSlot >= 0) {
				ItemStack returnNow = restoreSlot < ToolWheelState.SIZE
						? state.stacks.get(restoreSlot) : inv.getStack(restoreSlot);
				inv.setStack(handSlot, returnNow);
				if (restoreSlot < ToolWheelState.SIZE) {
					state.stacks.set(restoreSlot, handNow);
				} else if (wheelFree) {
					inv.setStack(restoreSlot, ItemStack.EMPTY);
					state.stacks.set(wheelSlot, handNow);
				} else {
					inv.setStack(restoreSlot, handNow);
				}
			} else {
				if (!wheelFree) return;
				Gestorage.LOGGER.warn("Auto Tool: session tool of {} is gone, parking the auto tool in wheel slot {}",
						player.getName().getString(), wheelSlot);
				player.sendMessage(Text.literal("§7Auto Tool: §6original tool missing, auto tool stored in the wheel"), true);
				parkInWheel(inv, state, handSlot, wheelSlot, handNow);
			}
		}
		inv.markDirty();
		state.scheduleSave();
		selectSlot(player, handSlot);
		ToolWheelSyncS2CPacket.sendTo(player);
	}

	private static void parkInWheel(PlayerInventory inv, ToolWheelState state, int handSlot, int wheelSlot, ItemStack handNow) {
		inv.setStack(handSlot, ItemStack.EMPTY);
		state.stacks.set(wheelSlot, handNow);
	}

	/**
	 * Looks for the session tool in the wheel first and then across the player
	 * inventory, returning its index or {@code -1} when it is gone. Matching runs from
	 * strictest to loosest (identical stack, same item and components, same item) so a
	 * tool that was used, repaired or re-enchanted while parked is still recognised,
	 * and the wheel slot the auto tool came from is always tried first. An empty
	 * session tool (the tool slot was empty when Auto Tool was activated) has nothing
	 * to restore.
	 */
	private static int findRestoreSlot(ToolWheelState state, PlayerInventory inv, ItemStack returnTool,
			int handSlot, int wheelSlot) {
		if (returnTool.isEmpty()) return -1;
		for (int level = 0; level < 3; level++) {
			if (wheelSlot >= 0 && wheelSlot < ToolWheelState.SIZE
					&& matches(state.stacks.get(wheelSlot), returnTool, level)) return wheelSlot;
			for (int i = 0; i < ToolWheelState.SIZE; i++) {
				if (i != wheelSlot && matches(state.stacks.get(i), returnTool, level)) return i;
			}
			for (int i = 0; i < PlayerInventory.MAIN_SIZE; i++) {
				if (i == handSlot) continue;
				if (matches(inv.getStack(i), returnTool, level)) return i;
			}
		}
		return -1;
	}

	private static boolean matches(ItemStack stack, ItemStack target, int level) {
		if (stack.isEmpty()) return false;
		return switch (level) {
			case 0 -> ItemStack.areEqual(stack, target);
			case 1 -> ItemStack.areItemsAndComponentsEqual(stack, target);
			default -> stack.getItem() == target.getItem();
		};
	}

	private static Session newSession(PlayerInventory inv, ToolWheelState state) {
		int handSlot = state != null ? ToolWheelLogic.resolveToolSlot(state, inv) : inv.selectedSlot;
		return new Session(handSlot, inv.getStack(handSlot).copy());
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
		ItemStack returnTool;
		volatile long lastMineTime = System.currentTimeMillis();
		int toolSlot = -1;
		int wheelSlot = -1;
		int handSlot = -1;
		Item toolItem = null;

		Session(int toolSlot, ItemStack returnTool) {
			this.toolSlot = toolSlot;
			this.returnTool = returnTool;
		}

		/**
		 * Re-captures the baseline when Auto Tool follows the selected slot and the
		 * player moved to another one, so the tool of the slot that is really being
		 * swapped is the one handed back. A pending swap keeps its own slot.
		 */
		void retarget(int handSlot, PlayerInventory inv) {
			if (hasPending() || this.toolSlot == handSlot) return;
			this.toolSlot = handSlot;
			returnTool = inv.getStack(handSlot).copy();
		}

		void refresh() {
			this.lastMineTime = System.currentTimeMillis();
		}

		void beginPending(int wheelSlot, int handSlot, ItemStack tool) {
			this.wheelSlot = wheelSlot;
			this.handSlot = handSlot;
			this.toolSlot = handSlot;
			this.toolItem = tool.getItem();
			this.lastMineTime = System.currentTimeMillis();
		}

		void clearPending() {
			this.wheelSlot = -1;
			this.handSlot = -1;
			this.toolItem = null;
		}

		boolean hasPending() {
			return toolItem != null;
		}
	}
}
