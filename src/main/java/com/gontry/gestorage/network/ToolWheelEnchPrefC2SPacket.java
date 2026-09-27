package com.gontry.gestorage.network;

import com.gontry.gestorage.ModConstants;
import com.gontry.gestorage.toolwheel.ToolWheelState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ToolWheelEnchPrefC2SPacket {
	public static void handle(ModNetworking.ToolWheelEnchPrefC2S payload, ServerPlayNetworking.Context ctx) {
		ctx.server().execute(() -> {
			ServerPlayerEntity player = ctx.player();
			if (player == null) return;
			int pref = payload.pref();
			if (pref < ModConstants.ENCH_PREF_NONE || pref > ModConstants.ENCH_PREF_MAX) return;
			ToolWheelState state = ToolWheelState.get(player);
			state.enchPref = pref;
			state.scheduleSave();
			ToolWheelEnchPrefS2CPacket.sendTo(player);
			player.sendMessage(Text.literal("§7Auto Tool Enchantment: " + label(pref)), true);
		});
	}

	public static String label(int pref) {
		return switch (pref) {
			case ModConstants.ENCH_PREF_FORTUNE -> "§dFortune";
			case ModConstants.ENCH_PREF_SILK_TOUCH -> "§fSilk Touch";
			default -> "§8None";
		};
	}
}
