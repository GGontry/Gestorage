package com.gontry.gestorage.network;

import com.gontry.gestorage.config.CarefulBreakServerConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CarefulBreakStateS2CPacket {
	private CarefulBreakStateS2CPacket() {}

	/** The player's own effective state; all off while they lack permission. */
	public static ModNetworking.CarefulBreakStateS2C buildState(ServerPlayerEntity player) {
		CarefulBreakServerConfig.Profile profile = CarefulBreakServerConfig.getEffectiveProfile(player);
		return new ModNetworking.CarefulBreakStateS2C(
				profile.enabled(),
				profile.carefulBreak(),
				profile.carefulDrop(),
				profile.alwaysCareful(),
				profile.treeCapitator(),
				profile.betterHarvesting(),
				profile.autoReplantTrees(),
				profile.autoReplantCrops()
		);
	}

	public static void sendTo(ServerPlayerEntity player) {
		ServerPlayNetworking.send(player, buildState(player));
	}

	public static void broadcast(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			sendTo(player);
		}
	}
}
