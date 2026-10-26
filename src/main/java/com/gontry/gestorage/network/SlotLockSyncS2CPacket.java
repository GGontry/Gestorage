package com.gontry.gestorage.network;

import com.gontry.gestorage.slotlock.SlotLockState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class SlotLockSyncS2CPacket {
	private SlotLockSyncS2CPacket() {}

	public static void sendTo(ServerPlayerEntity player) {
		SlotLockState state = SlotLockState.getExisting(player);
		int[] playerSlots = state == null ? new int[0] : state.playerSlotsSnapshot();
		int[] enderSlots = state == null ? new int[0] : state.enderSlotsSnapshot();
		ServerPlayNetworking.send(player, new ModNetworking.SlotLockSyncS2C(playerSlots, enderSlots));
	}
}
