package com.gontry.gestorage.client;

import com.gontry.gestorage.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ToolWheelEnchPrefS2CPacket {
	public static void handle(ModNetworking.ToolWheelEnchPrefS2C payload, ClientPlayNetworking.Context ctx) {
		ctx.client().execute(() -> ClientToolWheelState.applyEnchPref(payload.pref()));
	}
}
