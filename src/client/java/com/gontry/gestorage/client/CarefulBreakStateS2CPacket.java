package com.gontry.gestorage.client;

import com.gontry.gestorage.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class CarefulBreakStateS2CPacket {
	public static void handle(ModNetworking.CarefulBreakStateS2C payload, ClientPlayNetworking.Context ctx) {
		ctx.client().execute(() -> ClientCarefulBreakState.apply(
				payload.enabled(),
				payload.carefulBreak(),
				payload.carefulDrop(),
				payload.alwaysCareful(),
				payload.treeCapitator(),
				payload.betterHarvesting(),
				payload.autoReplant()
		));
	}
}
