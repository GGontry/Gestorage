package com.gontry.gestorage.client;

import com.gontry.gestorage.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ToolWheelSyncS2CPacket {
	public static void handle(ModNetworking.ToolWheelSyncS2C payload, ClientPlayNetworking.Context ctx) {
		ctx.client().execute(() -> ClientToolWheelState.apply(payload.stacks(), payload.autoTool()));
	}
}