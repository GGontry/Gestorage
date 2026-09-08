package com.gontry.gestorage.client;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class EnderSizeChangedS2CPacket {
	public static void handle(ModNetworking.EnderSizeChangedS2C payload, ClientPlayNetworking.Context ctx) {
		// Keep the client-side size mirror (ClientState) in agreement with the
		// server's ender chest gamerule. Must run on the client thread.
		ctx.client().execute(() -> {
			int newMode = payload.sizeMode();
			ClientState.setCachedSizeMode(newMode);
			Gestorage.LOGGER.debug("Client cached ender size mode updated to {}", newMode);
		});
	}
}
