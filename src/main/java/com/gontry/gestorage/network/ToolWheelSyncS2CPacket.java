package com.gontry.gestorage.network;

import com.gontry.gestorage.toolwheel.ToolWheelState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ToolWheelSyncS2CPacket {
	private ToolWheelSyncS2CPacket() {}

	public static ModNetworking.ToolWheelSyncS2C buildState(ServerPlayerEntity player) {
		ToolWheelState state = ToolWheelState.get(player);
		ItemStack[] stacks = new ItemStack[ToolWheelState.SIZE];
		for (int i = 0; i < ToolWheelState.SIZE; i++) {
			stacks[i] = state.stacks.get(i);
		}
		return new ModNetworking.ToolWheelSyncS2C(stacks, state.autoTool);
	}

	public static void sendTo(ServerPlayerEntity player) {
		if (player.networkHandler == null) return;
		ServerPlayNetworking.send(player, buildState(player));
	}

	public static void broadcast(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			sendTo(player);
		}
	}
}