package com.gontry.gestorage.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentStateManager;

import java.util.UUID;

public final class EnderChestFactory {
    private EnderChestFactory() {}

    public static EnhancedEnderChestInventory createForPlayer(ServerPlayerEntity player, int size) {
        PersistentStateManager stateManager = player.getWorld().getServer().getOverworld().getPersistentStateManager();
        return new EnhancedEnderChestInventory(
                player.getEnderChestInventory(), size, stateManager, player.getUuid()
        );
    }

    public static EnhancedEnderChestInventory createForPlayer(PlayerEntity player, int size, PersistentStateManager stateManager) {
        return new EnhancedEnderChestInventory(
                player.getEnderChestInventory(), size, stateManager, player.getUuid()
        );
    }
}
