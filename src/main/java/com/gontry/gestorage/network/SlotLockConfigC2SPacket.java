package com.gontry.gestorage.network;

import com.gontry.gestorage.slotlock.SlotLockManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class SlotLockConfigC2SPacket {
	private SlotLockConfigC2SPacket() {}

	public static void handle(ModNetworking.SlotLockConfigC2S payload, ServerPlayNetworking.Context ctx) {
		ctx.server().execute(() -> {
			ServerPlayerEntity player = ctx.player();
			if (player == null) return;

			SlotLockManager.setSettings(player, new SlotLockManager.Settings(
					payload.enabled(),
					payload.sortingSkipsLockedSlots(),
					payload.preventDrop(),
					payload.preventUse()
			));
			// Reply with the current locks so a client that just enabled the module
			// does not have to wait for the next join to draw the borders.
			SlotLockSyncS2CPacket.sendTo(player);
		});
	}
}
