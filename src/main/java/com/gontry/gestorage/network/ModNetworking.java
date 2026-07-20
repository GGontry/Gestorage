package com.gontry.gestorage.network;

import com.gontry.gestorage.Gestorage;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ModNetworking {
	public static final CustomPayload.Id<OpenEnderChestC2S> OPEN_ENDER_CHEST =
			new CustomPayload.Id<>(Identifier.of(Gestorage.MOD_ID, "open_ender_chest"));

	public static final CustomPayload.Id<OpenEnderScreenS2C> OPEN_ENDER_SCREEN =
			new CustomPayload.Id<>(Identifier.of(Gestorage.MOD_ID, "open_ender_screen"));

	public static final CustomPayload.Id<EnderSizeChangedS2C> ENDER_SIZE_CHANGED =
			new CustomPayload.Id<>(Identifier.of(Gestorage.MOD_ID, "ender_size_changed"));

	public static final CustomPayload.Id<RefillRequestC2S> REFILL_REQUEST =
			new CustomPayload.Id<>(Identifier.of(Gestorage.MOD_ID, "refill_request"));

	public static final PacketCodec<PacketByteBuf, OpenEnderChestC2S> OPEN_ENDER_CHEST_CODEC =
			PacketCodec.unit(new OpenEnderChestC2S());

	public static final PacketCodec<PacketByteBuf, OpenEnderScreenS2C> OPEN_ENDER_SCREEN_CODEC =
			PacketCodec.of(
					(OpenEnderScreenS2C p, PacketByteBuf buf) -> buf.writeInt(p.sizeMode()),
					buf -> new OpenEnderScreenS2C(buf.readInt())
			);

	public static final PacketCodec<PacketByteBuf, EnderSizeChangedS2C> ENDER_SIZE_CHANGED_CODEC =
			PacketCodec.of(
					(EnderSizeChangedS2C p, PacketByteBuf buf) -> buf.writeInt(p.sizeMode()),
					buf -> new EnderSizeChangedS2C(buf.readInt())
			);

	public static final PacketCodec<PacketByteBuf, RefillRequestC2S> REFILL_REQUEST_CODEC =
			PacketCodec.of(
					(RefillRequestC2S p, PacketByteBuf buf) -> {
						buf.writeInt(p.sourceSlot());
						buf.writeString(p.sourceType());
						buf.writeInt(p.targetSlot());
						buf.writeString(p.targetType());
					},
					buf -> new RefillRequestC2S(
						buf.readInt(),
						buf.readString(),
						buf.readInt(),
						buf.readString()
					)
			);

	public static void register() {
		PayloadTypeRegistry.playC2S().register(OPEN_ENDER_CHEST, OPEN_ENDER_CHEST_CODEC);
		PayloadTypeRegistry.playS2C().register(OPEN_ENDER_SCREEN, OPEN_ENDER_SCREEN_CODEC);
		PayloadTypeRegistry.playS2C().register(ENDER_SIZE_CHANGED, ENDER_SIZE_CHANGED_CODEC);
		PayloadTypeRegistry.playC2S().register(REFILL_REQUEST, REFILL_REQUEST_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(OPEN_ENDER_CHEST, OpenEnderChestC2SPacket::handle);
		ServerPlayNetworking.registerGlobalReceiver(REFILL_REQUEST, RefillRequestC2SPacket::handle);
	}

	public record OpenEnderChestC2S() implements CustomPayload {
		@Override
		public Id<? extends CustomPayload> getId() {
			return OPEN_ENDER_CHEST;
		}
	}

	public record OpenEnderScreenS2C(int sizeMode) implements CustomPayload {
		@Override
		public Id<? extends CustomPayload> getId() {
			return OPEN_ENDER_SCREEN;
		}
	}

	public record EnderSizeChangedS2C(int sizeMode) implements CustomPayload {
		@Override
		public Id<? extends CustomPayload> getId() {
			return ENDER_SIZE_CHANGED;
		}
	}

	public record RefillRequestC2S(int sourceSlot, String sourceType, int targetSlot, String targetType) implements CustomPayload {
		@Override
		public Id<? extends CustomPayload> getId() {
			return REFILL_REQUEST;
		}
	}
}
