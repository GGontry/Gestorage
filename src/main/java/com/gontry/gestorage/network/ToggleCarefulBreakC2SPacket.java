package com.gontry.gestorage.network;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.config.CarefulBreakServerConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ToggleCarefulBreakC2SPacket {
	public static void handle(ModNetworking.ToggleCarefulBreakC2S payload, ServerPlayNetworking.Context ctx) {
		ctx.server().execute(() -> {
			ServerPlayerEntity player = ctx.player();
			if (player == null) return;

			int option = payload.option();
			if (option < 0 || option > CarefulBreakServerConfig.OPTION_ENABLED) {
				Gestorage.LOGGER.warn("[ToggleCarefulBreak] Unknown option: {}", option);
				return;
			}

			if (!CarefulBreakServerConfig.isPermitted(player)) {
				player.sendMessage(Text.literal("§c[Gestorage] You don't have permission to change your Careful Break settings. "
						+ "Ask an operator to run /gestorage careful allow <player>."), false);
				// Force the client back to the effective (all-off) state.
				CarefulBreakStateS2CPacket.sendTo(player);
				return;
			}

			CarefulBreakServerConfig.Profile profile = CarefulBreakServerConfig.toggle(player, option);
			CarefulBreakStateS2CPacket.sendTo(player);
			player.sendMessage(Text.literal("§7" + optionName(option) + ": "
					+ (profile.option(option) ? "§aON" : "§cOFF")), true);
		});
	}

	private static String optionName(int option) {
		return switch (option) {
			case CarefulBreakServerConfig.OPTION_CAREFUL_BREAK -> "Careful Break";
			case CarefulBreakServerConfig.OPTION_CAREFUL_DROP -> "Careful Drop";
			case CarefulBreakServerConfig.OPTION_ALWAYS_CAREFUL -> "Always Careful";
			case CarefulBreakServerConfig.OPTION_TREE_CAPITATOR -> "Tree Capitator";
			case CarefulBreakServerConfig.OPTION_BETTER_HARVESTING -> "Better Harvesting";
			case CarefulBreakServerConfig.OPTION_AUTO_REPLANT_TREES -> "Auto Replant Trees";
			case CarefulBreakServerConfig.OPTION_AUTO_REPLANT_CROPS -> "Auto Replant Crops";
			case CarefulBreakServerConfig.OPTION_ENABLED -> "Enabled";
			default -> "Option";
		};
	}
}
