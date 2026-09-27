package com.gontry.gestorage.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Reserved receiver: the Auto Tool default tool was removed, Auto Tool now always
 * returns to the tool held when it was activated. The packet id stays registered so
 * the wire protocol of released versions is untouched.
 */
public class ToolWheelDefaultC2SPacket {
	public static void handle(ModNetworking.ToolWheelDefaultC2S payload, ServerPlayNetworking.Context ctx) {
	}
}
