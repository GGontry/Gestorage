package com.gontry.gestorage.client;

import net.minecraft.client.MinecraftClient;

public class WorldKeyHelper {
	public static String getFullWorldKey(MinecraftClient client) {
		if (client.world == null) return null;

		if (client.isIntegratedServerRunning() && client.getServer() != null) {
			return client.getServer().getSaveProperties().getLevelName();
		} else if (client.getCurrentServerEntry() != null) {
			return client.getCurrentServerEntry().address;
		}
		return "local";
	}
}
