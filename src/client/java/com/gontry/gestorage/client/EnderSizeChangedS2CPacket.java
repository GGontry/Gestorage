package com.gontry.gestorage.client;

import com.gontry.gestorage.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class EnderSizeChangedS2CPacket {
	public static void handle(ModNetworking.EnderSizeChangedS2C payload, ClientPlayNetworking.Context ctx) {
		ctx.client().execute(() -> {
			// Update cached size mode for keybind behavior
		});
	}
}
