package com.gontry.gestorage.network;

import com.gontry.gestorage.toolwheel.ToolWheelLogic;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ToolWheelSwapC2SPacket {
	public static void handle(ModNetworking.ToolWheelSwapC2S payload, ServerPlayNetworking.Context ctx) {
		ctx.server().execute(() -> {
			ServerPlayerEntity player = ctx.player();
			if (player == null) return;
			if (player.currentScreenHandler != player.playerScreenHandler) return;
			if (payload.slot() < 0 || payload.slot() >= 9) return;
			boolean changed = ToolWheelLogic.swapWithHand(player, payload.slot());
			if (changed) {
				ToolWheelSyncS2CPacket.sendTo(player);
				ItemStack hand = player.getInventory().getStack(player.getInventory().selectedSlot);
				if (!hand.isEmpty()) {
					player.sendMessage(Text.literal("§7Tool Wheel: §f").copy().append(hand.getName()), true);
				}
			}
		});
	}
}