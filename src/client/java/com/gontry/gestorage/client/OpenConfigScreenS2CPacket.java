package com.gontry.gestorage.client;

import com.gontry.gestorage.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class OpenConfigScreenS2CPacket {
	public static void handle(ModNetworking.OpenConfigScreenS2C payload, ClientPlayNetworking.Context ctx) {
		ctx.client().execute(() -> {
			ctx.client().setScreen(new GestorageConfigScreen(ctx.client().currentScreen));
		});
	}
}
