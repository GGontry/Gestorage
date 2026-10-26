package com.gontry.gestorage.client;

import com.gontry.gestorage.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class SlotLockSyncS2CPacket {
	public static void handle(ModNetworking.SlotLockSyncS2C payload, ClientPlayNetworking.Context ctx) {
		ctx.client().execute(() -> ClientSlotLockState.apply(payload.playerSlots(), payload.enderSlots()));
	}
}
