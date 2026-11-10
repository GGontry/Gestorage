package com.gontry.gestorage.network;

import com.gontry.gestorage.ModConstants;
import com.gontry.gestorage.ModGameRules;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Mirrors the server's {@code gestorage:enderChestSize} game rule to clients.
 *
 * <p>Minecraft 1.21.1 does not send game rules to clients, so without this the
 * client size mirror would only update when a chest is opened. The value is
 * pushed on join, whenever the rule changes ({@code /gamerule} included) and
 * after {@code /gestorage endersize}.
 */
public final class EnderSizeChangedS2CPacket {
	private static int lastBroadcast = ModConstants.MODE_NORMAL;

	private EnderSizeChangedS2CPacket() {}

	public static void sendTo(ServerPlayerEntity player) {
		ServerPlayNetworking.send(player, buildState(player.getServer()));
	}

	public static void broadcast(MinecraftServer server) {
		ModNetworking.EnderSizeChangedS2C payload = buildState(server);
		lastBroadcast = payload.sizeMode();
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public static void syncIfChanged(MinecraftServer server) {
		int current = ModGameRules.getEnderChestSize(server.getGameRules());
		if (current == lastBroadcast) return;
		broadcast(server);
	}

	public static void reset() {
		lastBroadcast = ModConstants.MODE_NORMAL;
	}

	private static ModNetworking.EnderSizeChangedS2C buildState(MinecraftServer server) {
		return new ModNetworking.EnderSizeChangedS2C(ModGameRules.getEnderChestSize(server.getGameRules()));
	}
}
