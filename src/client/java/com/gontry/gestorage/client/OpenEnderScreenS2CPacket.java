package com.gontry.gestorage.client;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class OpenEnderScreenS2CPacket {
	public static void handle(ModNetworking.OpenEnderScreenS2C payload, ClientPlayNetworking.Context ctx) {
		ctx.client().execute(() -> {
			// Reserved for future use.
			// Screen opening is currently handled by vanilla's openHandledScreen
			// triggered server-side. This packet could be used for custom screen
			// opening logic or pre-loading assets in the future.
			Gestorage.LOGGER.debug("Received open_ender_screen with sizeMode={}", payload.sizeMode());
		});
	}
}
