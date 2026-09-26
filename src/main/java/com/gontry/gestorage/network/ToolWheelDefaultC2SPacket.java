package com.gontry.gestorage.network;

import com.gontry.gestorage.ModConstants;
import com.gontry.gestorage.toolwheel.ToolWheelLogic;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ToolWheelDefaultC2SPacket {
	public static void handle(ModNetworking.ToolWheelDefaultC2S payload, ServerPlayNetworking.Context ctx) {
		ctx.server().execute(() -> {
			ServerPlayerEntity player = ctx.player();
			if (player == null) return;
			int slot = payload.slot();
			if (slot != ModConstants.TOOL_SLOT_NONE && !PlayerInventory.isValidHotbarIndex(slot)) return;
			if (slot == ModConstants.TOOL_SLOT_NONE) {
				ToolWheelLogic.clearDefaultTool(player);
				ToolWheelSyncS2CPacket.sendTo(player);
				player.sendMessage(Text.literal("§7Default Tool: §cNONE"), true);
				return;
			}
			if (!ToolWheelLogic.setDefaultTool(player, slot)) {
				player.sendMessage(Text.literal("§7Default Tool: §cEMPTY SLOT " + (slot + 1)), true);
				return;
			}
			ToolWheelSyncS2CPacket.sendTo(player);
			player.sendMessage(Text.literal("§7Default Tool: §f")
					.append(player.getInventory().getStack(slot).getName()), true);
		});
	}
}
