package com.gontry.gestorage.network;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.slotlock.SlotLockManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class SlotLockToggleC2SPacket {
	private SlotLockToggleC2SPacket() {}

	public static void handle(ModNetworking.SlotLockToggleC2S payload, ServerPlayNetworking.Context ctx) {
		ctx.server().execute(() -> {
			ServerPlayerEntity player = ctx.player();
			if (player == null) return;

			int scope = payload.scope();
			int slot = payload.slot();
			if (!SlotLockManager.hasVisibleSlot(player, scope, slot)) {
				Gestorage.LOGGER.warn("[SlotLock] Ignored toggle for invalid scope/slot {}:{}", scope, slot);
				return;
			}

			SlotLockManager.toggle(player, scope, slot);
			SlotLockSyncS2CPacket.sendTo(player);
		});
	}
}
