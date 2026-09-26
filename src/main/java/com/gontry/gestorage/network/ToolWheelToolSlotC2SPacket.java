package com.gontry.gestorage.network;

import com.gontry.gestorage.ModConstants;
import com.gontry.gestorage.toolwheel.ToolWheelLogic;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ToolWheelToolSlotC2SPacket {
	public static void handle(ModNetworking.ToolWheelToolSlotC2S payload, ServerPlayNetworking.Context ctx) {
		ctx.server().execute(() -> {
			ServerPlayerEntity player = ctx.player();
			if (player == null) return;
			int slot = payload.slot();
			if (slot != ModConstants.TOOL_SLOT_NONE && !PlayerInventory.isValidHotbarIndex(slot)) return;
			ToolWheelLogic.setDefaultSlot(player, slot);
			ToolWheelSyncS2CPacket.sendTo(player);
			player.sendMessage(Text.literal("§7Tool Slot: " + (slot == ModConstants.TOOL_SLOT_NONE
					? "§cSELECTED" : "§f" + (slot + 1))), true);
		});
	}
}
