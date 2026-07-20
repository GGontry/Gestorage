package com.gontry.gestorage.client;

import net.minecraft.client.MinecraftClient;

public class WorldKeyHelper {
	public static String getFullWorldKey(MinecraftClient client) {
		if (client.world == null) return null;

		String dimKey = client.world.getRegistryKey().getValue().toString();
		String saveId;

		if (client.isIntegratedServerRunning() && client.getServer() != null) {
			saveId = client.getServer().getSaveProperties().getLevelName();
		} else if (client.getCurrentServerEntry() != null) {
			saveId = client.getCurrentServerEntry().address;
		} else {
			saveId = "local";
		}

		return saveId + ":" + dimKey;
	}
}
