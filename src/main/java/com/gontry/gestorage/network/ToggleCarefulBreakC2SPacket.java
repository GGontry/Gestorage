package com.gontry.gestorage.network;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.config.CarefulBreakServerConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ToggleCarefulBreakC2SPacket {
	public static void handle(ModNetworking.ToggleCarefulBreakC2S payload, ServerPlayNetworking.Context ctx) {
		ServerPlayerEntity player = ctx.player();
		if (player == null) return;

		MinecraftServer server = player.getServer();
		boolean allowed = player.hasPermissionLevel(2)
				|| (server != null && server.isSingleplayer() && server.isHost(player.getGameProfile()));
		if (!allowed) {
			player.sendMessage(Text.literal("§cYou don't have permission to change Careful Break settings"), false);
			return;
		}

		String name;
		switch (payload.option()) {
			case 0 -> { name = "Careful Break"; CarefulBreakServerConfig.carefulBreak = !CarefulBreakServerConfig.carefulBreak; }
			case 1 -> { name = "Careful Drop"; CarefulBreakServerConfig.carefulDrop = !CarefulBreakServerConfig.carefulDrop; }
			case 2 -> { name = "Always Careful"; CarefulBreakServerConfig.alwaysCareful = !CarefulBreakServerConfig.alwaysCareful; }
			case 3 -> { name = "Tree Capitator"; CarefulBreakServerConfig.treeCapitator = !CarefulBreakServerConfig.treeCapitator; }
			case 4 -> { name = "Better Harvesting"; CarefulBreakServerConfig.betterHarvesting = !CarefulBreakServerConfig.betterHarvesting; }
			case 5 -> { name = "Auto Replant"; CarefulBreakServerConfig.autoReplant = !CarefulBreakServerConfig.autoReplant; }
			case 6 -> { name = "Enabled"; CarefulBreakServerConfig.enabled = !CarefulBreakServerConfig.enabled; }
			default -> {
				Gestorage.LOGGER.warn("[ToggleCarefulBreak] Unknown option: {}", payload.option());
				return;
			}
		}
		CarefulBreakServerConfig.save();

		if (server != null) {
			CarefulBreakStateS2CPacket.broadcast(server);
		}
		player.sendMessage(Text.literal("§7" + name + ": " + (isOptionEnabled(payload.option()) ? "§aON" : "§cOFF")), true);
	}

	private static boolean isOptionEnabled(int option) {
		return switch (option) {
			case 0 -> CarefulBreakServerConfig.carefulBreak;
			case 1 -> CarefulBreakServerConfig.carefulDrop;
			case 2 -> CarefulBreakServerConfig.alwaysCareful;
			case 3 -> CarefulBreakServerConfig.treeCapitator;
			case 4 -> CarefulBreakServerConfig.betterHarvesting;
			case 5 -> CarefulBreakServerConfig.autoReplant;
			default -> CarefulBreakServerConfig.enabled;
		};
	}
}
