package com.gontry.gestorage.network;

import com.gontry.gestorage.ModGameRules;
import com.gontry.gestorage.shulker.LithiumHopperCache;
import com.gontry.gestorage.shulker.ShulkerStackNormalizer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ShulkerStackStateS2CPacket {
	private static boolean lastBroadcast;
	private static boolean lastHoppersBroadcast;

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
	 * Pushes both rules to every client whenever either changes, so a plain vanilla
	 * {@code /gamerule} is mirrored just like the {@code /gestorage shulkerstack}
	 * command. A change can make Lithium's cached full-slot counts stale, and
	 * turning player stacking off also has to split stacks that no longer fit.
	 */
	public static void syncIfChanged(MinecraftServer server) {
		boolean players = ModGameRules.isStackableShulkers(server.getGameRules());
		boolean hoppers = ModGameRules.isHopperStackableShulkers(server.getGameRules());
		if (players == lastBroadcast && hoppers == lastHoppersBroadcast) return;

		boolean playersTurnedOff = lastBroadcast && !players;
		lastBroadcast = players;
		lastHoppersBroadcast = hoppers;

		LithiumHopperCache.invalidateAll();
		if (playersTurnedOff) {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				ShulkerStackNormalizer.normalize(player);
			}
		}
		broadcast(server);
	}

	public static void reset() {
		lastBroadcast = false;
		lastHoppersBroadcast = false;
	}

	private static ModNetworking.ShulkerStackStateS2C buildState(MinecraftServer server) {
		return new ModNetworking.ShulkerStackStateS2C(
				ModGameRules.isStackableShulkers(server.getGameRules()),
				ModGameRules.isHopperStackableShulkers(server.getGameRules())
		);
	}
}
