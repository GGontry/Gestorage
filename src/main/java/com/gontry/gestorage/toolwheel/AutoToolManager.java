package com.gontry.gestorage.toolwheel;

import com.gontry.gestorage.network.ToolWheelSyncS2CPacket;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
		ItemStack hand = inv.getStack(inv.selectedSlot);
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
		inv.setStack(inv.selectedSlot, tool);
		state.stacks.set(bestSlot, hand);
		inv.markDirty();
		state.scheduleSave();
		ToolWheelSyncS2CPacket.sendTo(player);

		return new ActiveSwap(bestSlot, hand, tool);
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
		ItemStack hand = player.getInventory().getStack(player.getInventory().selectedSlot);
		return !hand.isEmpty() && hand.isOf(swap.toolItem) && hand.getCount() == swap.toolCount;
	}

	private static void revert(ServerPlayerEntity player, ActiveSwap swap) {
		ACTIVE.remove(player.getUuid());
		ToolWheelState state = ToolWheelState.getExisting(player);
		if (state == null) return;
		PlayerInventory inv = player.getInventory();
		ItemStack handNow = inv.getStack(inv.selectedSlot);
		if (handNow.isEmpty() || !handNow.isOf(swap.toolItem) || handNow.getCount() != swap.toolCount) return;

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
		inv.setStack(inv.selectedSlot, wheelNow);
		state.stacks.set(restoreSlot, handNow);
		inv.markDirty();
		state.scheduleSave();
		ToolWheelSyncS2CPacket.sendTo(player);
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

	public static void clear(UUID playerUuid) {
		ACTIVE.remove(playerUuid);
	}

	public static void clearAll() {
		ACTIVE.clear();
	}

	private static final class ActiveSwap {
		final int wheelSlot;
		final ItemStack original;
		final Item toolItem;
		final int toolCount;
		ItemStack sessionOriginal;
		volatile long lastMineTime = System.currentTimeMillis();

		ActiveSwap(int wheelSlot, ItemStack original, ItemStack tool) {
			this.wheelSlot = wheelSlot;
			this.original = original;
			this.toolItem = tool.getItem();
			this.toolCount = tool.getCount();
		}
	}
}