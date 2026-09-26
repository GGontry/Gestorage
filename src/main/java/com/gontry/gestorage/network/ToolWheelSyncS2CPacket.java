package com.gontry.gestorage.network;

import com.gontry.gestorage.ModConstants;
import com.gontry.gestorage.toolwheel.ToolWheelState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ToolWheelSyncS2CPacket {
	private ToolWheelSyncS2CPacket() {}

	public static ModNetworking.ToolWheelSyncS2C buildState(ServerPlayerEntity player) {
		ToolWheelState state = ToolWheelState.getExisting(player);
		ItemStack[] stacks = new ItemStack[ToolWheelState.SIZE];
		if (state == null) {
			for (int i = 0; i < ToolWheelState.SIZE; i++) {
				stacks[i] = ItemStack.EMPTY;
			}
			return new ModNetworking.ToolWheelSyncS2C(stacks, false, ItemStack.EMPTY, ModConstants.TOOL_SLOT_NONE);
		}
		for (int i = 0; i < ToolWheelState.SIZE; i++) {
			stacks[i] = state.stacks.get(i);
		}
		return new ModNetworking.ToolWheelSyncS2C(stacks, state.autoTool, state.defaultTool, state.defaultSlot);
	}

	public static void sendTo(ServerPlayerEntity player) {
		if (player.networkHandler == null) return;
		ServerPlayNetworking.send(player, buildState(player));
	}
}