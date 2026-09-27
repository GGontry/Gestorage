package com.gontry.gestorage.network;

import com.gontry.gestorage.toolwheel.AutoToolManager;
import com.gontry.gestorage.toolwheel.ToolWheelState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Keeps the server-authoritative Auto Tool in step with the client module flag: the
 * Tool Wheel module is a client config, but Auto Tool runs on the server, so
 * switching the module off has to switch Auto Tool off as well or tools would keep
 * jumping into the hand of a player who just turned the module off.
 */
public class ToolWheelModuleC2SPacket {
	public static void handle(ModNetworking.ToolWheelModuleC2S payload, ServerPlayNetworking.Context ctx) {
		ctx.server().execute(() -> {
			ServerPlayerEntity player = ctx.player();
			if (player == null) return;
			AutoToolManager.setModuleEnabled(player, payload.enabled());
			ToolWheelState state = ToolWheelState.getExisting(player);
			boolean autoToolStopped = false;
			if (!payload.enabled() && state != null && state.autoTool) {
				state.autoTool = false;
				state.scheduleSave();
				autoToolStopped = true;
			}
			if (payload.enabled() || state != null) {
				ToolWheelSyncS2CPacket.sendTo(player);
			}
			player.sendMessage(Text.literal(autoToolStopped
					? "§7Tool Wheel: §cOFF §8(Auto Tool disabled)"
					: "§7Tool Wheel: " + (payload.enabled() ? "§aON" : "§cOFF")), true);
		});
	}
}
