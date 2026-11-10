package com.gontry.gestorage.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.PersistentStateManager;

/**
 * Single construction point for {@link EnhancedEnderChestInventory}.
 *
 * Both the server packet path and the sorting path create ender chest wrappers
 * through this factory, so the overflow consistency logic applied by
 * {@code EnhancedEnderChestInventory} is never bypassed by a new call site.
 * It is also the place where the once-per-session overflow backup is taken,
 * before any wrapper can read or write that player's state.
 */
public final class EnderChestFactory {
	private EnderChestFactory() {}

	/** Server-side entry point; resolves the overworld persistent state manager. */
	public static EnhancedEnderChestInventory createForPlayer(ServerPlayerEntity player, int size) {
		backupOverflowOnce(player);
		PersistentStateManager stateManager = player.getWorld().getServer().getOverworld().getPersistentStateManager();
		return new EnhancedEnderChestInventory(
				player.getEnderChestInventory(), size, stateManager, player.getUuid()
		);
	}

	/** Variant for call sites that already hold a state manager (e.g. sorting). */
	public static EnhancedEnderChestInventory createForPlayer(PlayerEntity player, int size, PersistentStateManager stateManager) {
		backupOverflowOnce(player);
		return new EnhancedEnderChestInventory(
				player.getEnderChestInventory(), size, stateManager, player.getUuid()
		);
	}

	/**
	 * Copies the player's stored overflow NBT the first time it is touched in a
	 * session. {@link EnderOverflowState#sessionBackup} is idempotent, so calling it
	 * on every wrapper creation costs one set lookup.
	 */
	private static void backupOverflowOnce(PlayerEntity player) {
		MinecraftServer server = player.getServer();
		if (server == null) return;
		EnderOverflowState.sessionBackup(server.getSavePath(WorldSavePath.ROOT), player.getUuid());
	}
}