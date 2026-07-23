package com.gontry.gestorage.network;

import com.gontry.gestorage.ModConstants;
import com.gontry.gestorage.ModGameRules;
import com.gontry.gestorage.inventory.EnderChestFactory;
import com.gontry.gestorage.menu.ExtraLargeEnderMenu;
import com.gontry.gestorage.menu.LargeEnderMenu;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class OpenEnderChestC2SPacket {
	public static void handle(ModNetworking.OpenEnderChestC2S payload, net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context ctx) {
		ctx.server().execute(() -> {
			var player = ctx.player();
			var gameRules = player.getWorld().getGameRules();

			int sizeMode = ModGameRules.getEnderChestSize(gameRules);

			switch (sizeMode) {
				case ModConstants.MODE_LARGE -> {
					var enhancedInv = EnderChestFactory.createForPlayer(player, ModConstants.LARGE_ENDER_SIZE);
					player.openHandledScreen(new ExtendedScreenHandlerFactory<>() {
						@Override
						public PacketByteBuf getScreenOpeningData(ServerPlayerEntity player1) {
							return new PacketByteBuf(Unpooled.buffer());
						}

						@Override
						public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player1) {
							return new LargeEnderMenu(syncId, playerInventory, enhancedInv);
						}

						@Override
						public Text getDisplayName() {
							return Text.translatable("container.gestorage.large_ender");
						}
					});
				}
				case ModConstants.MODE_EXTRA_LARGE -> {
					var enhancedInv = EnderChestFactory.createForPlayer(player, ModConstants.EXTRA_LARGE_ENDER_SIZE);
					player.openHandledScreen(new ExtendedScreenHandlerFactory<>() {
						@Override
						public PacketByteBuf getScreenOpeningData(ServerPlayerEntity player1) {
							return new PacketByteBuf(Unpooled.buffer());
						}

						@Override
						public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player1) {
							return new ExtraLargeEnderMenu(syncId, playerInventory, enhancedInv);
						}

						@Override
						public Text getDisplayName() {
							return Text.translatable("container.gestorage.extra_large_ender");
						}
					});
				}
				default -> {
					var enderChest = player.getEnderChestInventory();
					player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory(
							(syncId, inventory, p) -> GenericContainerScreenHandler.createGeneric9x3(syncId, inventory, enderChest),
							Text.translatable("container.enderchest")
					));
				}
			}
		});
	}
}
