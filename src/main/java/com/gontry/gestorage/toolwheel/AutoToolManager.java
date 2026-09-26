package com.gontry.gestorage.toolwheel;

import com.gontry.gestorage.network.ToolWheelSyncS2CPacket;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoToolManager {
	private static final long REVERT_DELAY_MS = 1000L;
	private static final Map<UUID, ActiveSwap> ACTIVE = new ConcurrentHashMap<>();

	private AutoToolManager() {}

	public static void onMineStart(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		if (player.isCreative() || player.isSpectator()) return;
		UUID id = player.getUuid();
		ActiveSwap created = tryAutoSwap(player, world, pos);
		if (created != null) {
			ActiveSwap prev = ACTIVE.get(id);
			created.sessionOriginal = prev != null ? prev.sessionOriginal : created.original;
			ACTIVE.put(id, created);
			return;
		}
		ActiveSwap prev = ACTIVE.get(id);
		if (prev != null && player.isAlive() && handHoldsTool(player, prev)) {
			prev.lastMineTime = System.currentTimeMillis();
		} else {
			ACTIVE.remove(id);
		}
	}

	public static void refresh(ServerPlayerEntity player) {
		ActiveSwap swap = ACTIVE.get(player.getUuid());
		if (swap != null) swap.lastMineTime = System.currentTimeMillis();
	}

	private static ActiveSwap tryAutoSwap(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		ToolWheelState state = ToolWheelState.getExisting(player);
		if (state == null || !state.autoTool) return null;
		BlockState target = world.getBlockState(pos);
		if (target.isAir()) return null;

		PlayerInventory inv = player.getInventory();
		int handSlot = ToolWheelLogic.resolveToolSlot(state, inv);
		ItemStack hand = inv.getStack(handSlot);
		RegistryEntry<Enchantment> efficiency = player.getServer().getRegistryManager()
				.get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.EFFICIENCY).orElse(null);
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
		if (bestSlot < 0) return null;

		ItemStack tool = state.stacks.get(bestSlot);
		inv.setStack(handSlot, tool);
		state.stacks.set(bestSlot, hand);
		inv.markDirty();
		state.scheduleSave();
		selectSlot(player, handSlot);
		ToolWheelSyncS2CPacket.sendTo(player);

		return new ActiveSwap(bestSlot, handSlot, hand, tool);
	}

	public static void tick(ServerPlayerEntity player) {
		UUID id = player.getUuid();
		ActiveSwap swap = ACTIVE.get(id);
		if (swap == null) return;
		if (!player.isAlive()) {
			ACTIVE.remove(id);
			return;
		}
		if (System.currentTimeMillis() - swap.lastMineTime >= REVERT_DELAY_MS) {
			revert(player, swap);
		}
	}

	private static boolean handHoldsTool(ServerPlayerEntity player, ActiveSwap swap) {
		ItemStack hand = player.getInventory().getStack(swap.handSlot);
		return !hand.isEmpty() && hand.isOf(swap.toolItem) && hand.getCount() == swap.toolCount;
	}

	private static void revert(ServerPlayerEntity player, ActiveSwap swap) {
		ACTIVE.remove(player.getUuid());
		ToolWheelState state = ToolWheelState.getExisting(player);
		if (state == null) return;
		PlayerInventory inv = player.getInventory();
		ItemStack handNow = inv.getStack(swap.handSlot);
		if (handNow.isEmpty() || !handNow.isOf(swap.toolItem) || handNow.getCount() != swap.toolCount) return;

		if (restoreDefaultTool(player, state, inv, swap.handSlot)) return;

		int restoreSlot;
		if (!swap.sessionOriginal.isEmpty()) {
			restoreSlot = findSlot(state, swap.sessionOriginal);
			if (restoreSlot < 0 && swap.wheelSlot >= 0 && swap.wheelSlot < ToolWheelState.SIZE
					&& state.stacks.get(swap.wheelSlot).isEmpty()) {
				restoreSlot = swap.wheelSlot;
			}
		} else {
			restoreSlot = swap.wheelSlot < ToolWheelState.SIZE ? swap.wheelSlot : -1;
			if (restoreSlot >= 0 && !state.stacks.get(restoreSlot).isEmpty()) restoreSlot = -1;
		}
		if (restoreSlot < 0) return;

		ItemStack wheelNow = state.stacks.get(restoreSlot);
		inv.setStack(swap.handSlot, wheelNow);
		state.stacks.set(restoreSlot, handNow);
		inv.markDirty();
		state.scheduleSave();
		selectSlot(player, swap.handSlot);
		ToolWheelSyncS2CPacket.sendTo(player);
	}

	/**
	 * Swaps the tool the player chose as default back into {@code handSlot}, looking
	 * for it in the wheel first and then across the whole player inventory. Counts
	 * are ignored while matching so a default tool that was restocked still counts.
	 */
	private static boolean restoreDefaultTool(ServerPlayerEntity player, ToolWheelState state,
			PlayerInventory inv, int handSlot) {
		if (state.defaultTool.isEmpty()) return false;
		for (int i = 0; i < ToolWheelState.SIZE; i++) {
			if (isDefaultTool(state.stacks.get(i), state.defaultTool)) {
				ItemStack toolNow = inv.getStack(handSlot);
				inv.setStack(handSlot, state.stacks.get(i));
				state.stacks.set(i, toolNow);
				inv.markDirty();
				state.scheduleSave();
				selectSlot(player, handSlot);
				ToolWheelSyncS2CPacket.sendTo(player);
				return true;
			}
		}
		for (int i = 0; i < PlayerInventory.MAIN_SIZE; i++) {
			if (i == handSlot || !isDefaultTool(inv.getStack(i), state.defaultTool)) continue;
			ItemStack toolNow = inv.getStack(handSlot);
			inv.setStack(handSlot, inv.getStack(i));
			inv.setStack(i, toolNow);
			inv.markDirty();
			selectSlot(player, handSlot);
			ToolWheelSyncS2CPacket.sendTo(player);
			return true;
		}
		return false;
	}

	private static boolean isDefaultTool(ItemStack stack, ItemStack defaultTool) {
		return !stack.isEmpty() && ItemStack.areItemsAndComponentsEqual(stack, defaultTool);
	}

	private static int findSlot(ToolWheelState state, ItemStack stack) {
		for (int i = 0; i < ToolWheelState.SIZE; i++) {
			if (ItemStack.areEqual(state.stacks.get(i), stack)) return i;
		}
		return -1;
	}

	private static float miningSpeed(ItemStack stack, BlockState target, RegistryEntry<Enchantment> efficiency) {
		float speed = stack.getMiningSpeedMultiplier(target);
		if (efficiency != null) {
			int efficiencyLevel = EnchantmentHelper.getLevel(efficiency, stack);
			if (efficiencyLevel > 0) {
				speed += efficiencyLevel * efficiencyLevel + 1.0F;
			}
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
		ACTIVE.remove(playerUuid);
	}

	public static void clearAll() {
		ACTIVE.clear();
	}

	private static final class ActiveSwap {
		final int wheelSlot;
		final int handSlot;
		final ItemStack original;
		final Item toolItem;
		final int toolCount;
		ItemStack sessionOriginal;
		volatile long lastMineTime = System.currentTimeMillis();

		ActiveSwap(int wheelSlot, int handSlot, ItemStack original, ItemStack tool) {
			this.wheelSlot = wheelSlot;
			this.handSlot = handSlot;
			this.original = original;
			this.toolItem = tool.getItem();
			this.toolCount = tool.getCount();
		}
	}
}
