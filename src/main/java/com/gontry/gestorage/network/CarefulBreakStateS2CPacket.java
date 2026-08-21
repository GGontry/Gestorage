package com.gontry.gestorage.network;

import com.gontry.gestorage.config.CarefulBreakServerConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CarefulBreakStateS2CPacket {
	private CarefulBreakStateS2CPacket() {}

	public static ModNetworking.CarefulBreakStateS2C buildState() {
		return new ModNetworking.CarefulBreakStateS2C(
				CarefulBreakServerConfig.enabled,
				CarefulBreakServerConfig.carefulBreak,
				CarefulBreakServerConfig.carefulDrop,
				CarefulBreakServerConfig.alwaysCareful,
				CarefulBreakServerConfig.treeCapitator,
				CarefulBreakServerConfig.betterHarvesting,
				CarefulBreakServerConfig.autoReplant
		);
	}

	public static void sendTo(ServerPlayerEntity player) {
		ServerPlayNetworking.send(player, buildState());
	}

	public static void broadcast(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			sendTo(player);
		}
	}
}
