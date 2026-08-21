package com.gontry.gestorage.careful;

import com.gontry.gestorage.config.CarefulBreakServerConfig;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public final class CarefulBreakManager {
	public static volatile boolean suppressBlockEffects = false;

	private CarefulBreakManager() {}

	public static boolean shouldCollectBlockDrops(ServerPlayerEntity player) {
		if (!CarefulBreakServerConfig.enabled) return false;
		boolean cbActive = CarefulBreakServerConfig.carefulBreak && player.isSneaking();
		boolean acActive = CarefulBreakServerConfig.alwaysCareful;
		return cbActive || acActive;
	}

	public static boolean shouldCollectEntityDrops(ServerPlayerEntity player) {
		if (!CarefulBreakServerConfig.enabled) return false;
		boolean cdActive = CarefulBreakServerConfig.carefulDrop && player.isSneaking();
		boolean acActive = CarefulBreakServerConfig.alwaysCareful;
		return cdActive || acActive;
	}

	public static boolean isTreeCapitatorActive(ServerPlayerEntity player) {
		if (!CarefulBreakServerConfig.enabled) return false;
		if (!CarefulBreakServerConfig.treeCapitator) return false;
		return shouldCollectBlockDrops(player);
	}

	public static boolean isBetterHarvestingActive(ServerPlayerEntity player) {
		if (!CarefulBreakServerConfig.enabled) return false;
		if (!CarefulBreakServerConfig.betterHarvesting) return false;
		return shouldCollectBlockDrops(player);
	}

	public static void collectDrops(ServerPlayerEntity player, List<ItemStack> drops, World world, BlockPos pos) {
		PlayerInventory inv = player.getInventory();
		for (ItemStack drop : drops) {
			if (!inv.insertStack(drop)) {
				ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
				entity.setVelocity(0, 0, 0);
				world.spawnEntity(entity);
			}
		}
	}

	public static void collectDrops(ServerPlayerEntity player, List<ItemStack> drops) {
		PlayerInventory inv = player.getInventory();
		for (ItemStack drop : drops) {
			if (!inv.insertStack(drop)) {
				player.dropItem(drop, false);
			}
		}
	}
}
