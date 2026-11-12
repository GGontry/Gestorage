package com.gontry.gestorage.client;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class EnderSizeChangedS2CPacket {
	public static void handle(ModNetworking.EnderSizeChangedS2C payload, ClientPlayNetworking.Context ctx) {
		// The server opens the matching menu, so the client only needs the value for
		// diagnostics; keep it on the client thread like every other S2C handler.
		ctx.client().execute(() -> Gestorage.LOGGER.debug("Server ender chest size mode: {}", payload.sizeMode()));
	}
}
