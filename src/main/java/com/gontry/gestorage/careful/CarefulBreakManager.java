package com.gontry.gestorage.careful;

import com.gontry.gestorage.config.CarefulBreakServerConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public final class CarefulBreakManager {
	public static volatile boolean suppressBlockEffects = false;

	/**
	 * One frame per death currently in progress. Frames nest because a death can
	 * trigger another death (or a vehicle damage) from inside its own callbacks.
	 */
	public record DeathContext(ServerPlayerEntity killer, Entity victim, DeathContext previous) {}

	private static final ThreadLocal<DeathContext> CURRENT_DEATH = new ThreadLocal<>();

	private CarefulBreakManager() {}

	/**
	 * Pushes a death frame for every player kill, even when drops are not being
	 * collected, so {@link #endDeath(Entity)} can always unwind exactly one frame.
	 */
	public static void beginDeath(ServerPlayerEntity killer, Entity victim) {
		CURRENT_DEATH.set(new DeathContext(killer, victim, CURRENT_DEATH.get()));
	}

	/**
	 * Nearest enclosing frame whose killer collects entity drops, or {@code null}.
	 * Callers still check the victim, so an inner non-collecting death never
	 * attributes its drops to an outer victim.
	 */
	public static DeathContext getCurrentDeath() {
		DeathContext ctx = CURRENT_DEATH.get();
		while (ctx != null) {
			if (shouldCollectEntityDrops(ctx.killer())) return ctx;
			ctx = ctx.previous();
		}
		return null;
	}

	/**
	 * Pops the frame belonging to {@code victim}. Deaths without a player attacker
	 * never push a frame, so they must not pop an enclosing one.
	 */
	public static void endDeath(Entity victim) {
		DeathContext ctx = CURRENT_DEATH.get();
		while (ctx != null) {
			if (ctx.victim() == victim) {
				if (ctx.previous() == null) {
					CURRENT_DEATH.remove();
				} else {
					CURRENT_DEATH.set(ctx.previous());
				}
				return;
			}
			ctx = ctx.previous();
		}
	}

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

	public static ItemEntity collectOrSpawn(ServerPlayerEntity player, ItemStack stack) {
		if (stack.isEmpty()) return null;
		if (player.getInventory().insertStack(stack)) return null;
		return player.dropItem(stack, false);
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