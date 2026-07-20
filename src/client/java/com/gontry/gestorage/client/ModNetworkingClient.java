package com.gontry.gestorage.client;

import com.gontry.gestorage.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ModNetworkingClient {
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_ENDER_SCREEN, OpenEnderScreenS2CPacket::handle);
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.ENDER_SIZE_CHANGED, EnderSizeChangedS2CPacket::handle);
	}

	public static void sendOpenEnderChest() {
		ClientPlayNetworking.send(new ModNetworking.OpenEnderChestC2S());
	}
}
