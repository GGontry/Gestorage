package com.gontry.gestorage.network;

import com.gontry.gestorage.toolwheel.ToolWheelState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ToolWheelAutoC2SPacket {
	public static void handle(ModNetworking.ToolWheelAutoC2S payload, ServerPlayNetworking.Context ctx) {
		ctx.server().execute(() -> {
			ServerPlayerEntity player = ctx.player();
			if (player == null) return;
			ToolWheelState state = ToolWheelState.get(player);
			state.autoTool = !state.autoTool;
			state.scheduleSave();
			ToolWheelSyncS2CPacket.sendTo(player);
			player.sendMessage(Text.literal(
					"§7Auto Tool: " + (state.autoTool ? "§aON" : "§cOFF")), true);
		});
	}
}