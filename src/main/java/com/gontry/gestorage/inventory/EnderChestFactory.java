package com.gontry.gestorage.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentStateManager;

/**
 * Single construction point for {@link EnhancedEnderChestInventory}.
 *
 * Both the server packet path and the sorting path create ender chest wrappers
 * through this factory, so the overflow consistency logic applied by
 * {@code EnhancedEnderChestInventory} is never bypassed by a new call site.
 */
public final class EnderChestFactory {
	private EnderChestFactory() {}

	/** Server-side entry point; resolves the overworld persistent state manager. */
	public static EnhancedEnderChestInventory createForPlayer(ServerPlayerEntity player, int size) {
		PersistentStateManager stateManager = player.getWorld().getServer().getOverworld().getPersistentStateManager();
		return new EnhancedEnderChestInventory(
				player.getEnderChestInventory(), size, stateManager, player.getUuid()
		);
	}

	/** Variant for call sites that already hold a state manager (e.g. sorting). */
	public static EnhancedEnderChestInventory createForPlayer(PlayerEntity player, int size, PersistentStateManager stateManager) {
		return new EnhancedEnderChestInventory(
				player.getEnderChestInventory(), size, stateManager, player.getUuid()
		);
	}
}