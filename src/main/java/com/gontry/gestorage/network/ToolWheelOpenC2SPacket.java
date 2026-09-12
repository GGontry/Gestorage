package com.gontry.gestorage.network;

import com.gontry.gestorage.toolwheel.ToolWheelInventory;
import com.gontry.gestorage.toolwheel.ToolWheelState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ToolWheelOpenC2SPacket {
	public static void handle(ModNetworking.ToolWheelOpenC2S payload, ServerPlayNetworking.Context ctx) {
		ctx.server().execute(() -> {
			ServerPlayerEntity player = ctx.player();
			if (player == null) return;
			ToolWheelState state = ToolWheelState.get(player);
			ToolWheelInventory inv = new ToolWheelInventory(state, player);
			player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
					(syncId, playerInventory, p) -> new GenericContainerScreenHandler(
							ScreenHandlerType.GENERIC_9X1, syncId, playerInventory, inv, 1),
					Text.translatable("container.gestorage.tool_wheel")));
		});
	}
}