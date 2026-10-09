package com.gontry.gestorage.client;

import com.gontry.gestorage.network.ModNetworking;
import com.gontry.gestorage.shulker.ShulkerStackRule;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ShulkerStackStateS2CPacket {
	public static void handle(ModNetworking.ShulkerStackStateS2C payload, ClientPlayNetworking.Context ctx) {
		ctx.client().execute(() -> ShulkerStackRule.setClientMirror(payload.enabled()));
	}
}
