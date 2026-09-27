package com.gontry.gestorage.network;

import com.gontry.gestorage.ModConstants;
import com.gontry.gestorage.toolwheel.ToolWheelState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public class ToolWheelEnchPrefS2CPacket {
	private ToolWheelEnchPrefS2CPacket() {}

	public static void sendTo(ServerPlayerEntity player) {
		if (player.networkHandler == null) return;
		ToolWheelState state = ToolWheelState.getExisting(player);
		int pref = state != null && state.enchPref >= ModConstants.ENCH_PREF_NONE
				&& state.enchPref <= ModConstants.ENCH_PREF_MAX ? state.enchPref : ModConstants.ENCH_PREF_NONE;
		ServerPlayNetworking.send(player, new ModNetworking.ToolWheelEnchPrefS2C(pref));
	}
}
