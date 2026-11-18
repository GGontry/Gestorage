package com.gontry.gestorage.client;

import com.gontry.gestorage.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Applies only the reply matching the latest request, so stale targets never flash. */
public class InspectorDataS2CPacket {
	private InspectorDataS2CPacket() {}

	public static void handle(ModNetworking.InspectDataS2C payload, ClientPlayNetworking.Context ctx) {
		ctx.client().execute(() -> {
			if (payload.requestId() != ClientInspectorState.expectedRequestId) return;
			InspectorTickHandler.onReply(payload);
		});
	}
}
