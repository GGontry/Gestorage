package com.gontry.gestorage.network;

import com.gontry.gestorage.ModGameRules;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ShulkerStackStateS2CPacket {
	private static boolean lastBroadcast;

	private ShulkerStackStateS2CPacket() {}

	public static void sendTo(ServerPlayerEntity player) {
		ServerPlayNetworking.send(player, buildState(player.getServer()));
	}

	public static void broadcast(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			ServerPlayNetworking.send(player, buildState(server));
		}
	}

	/**
	 * Pushes the rule to every client whenever it changes, so a plain vanilla
	 * {@code /gamerule gestorage:stackableShulkers} is mirrored just like the
	 * {@code /gestorage shulkerstack} command.
	 */
	public static void syncIfChanged(MinecraftServer server) {
		boolean current = ModGameRules.isStackableShulkers(server.getGameRules());
		if (current == lastBroadcast) return;
		lastBroadcast = current;
		broadcast(server);
	}

	public static void reset() {
		lastBroadcast = false;
	}

	private static ModNetworking.ShulkerStackStateS2C buildState(MinecraftServer server) {
		return new ModNetworking.ShulkerStackStateS2C(ModGameRules.isStackableShulkers(server.getGameRules()));
	}
}
