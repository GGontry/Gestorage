package com.gontry.gestorage.client;

import com.gontry.gestorage.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class OpenEnderScreenS2CPacket {
	public static void handle(ModNetworking.OpenEnderScreenS2C payload, ClientPlayNetworking.Context ctx) {
		ctx.client().execute(() -> {
			// Screen opening is handled by vanilla's openHandledScreen
			// This packet is for future use
		});
	}
}
