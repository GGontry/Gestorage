package com.gontry.gestorage.mixin;

import com.gontry.gestorage.ModGameRules;
import com.gontry.gestorage.inventory.EnhancedEnderChestInventory;
import com.gontry.gestorage.menu.ExtraLargeEnderMenu;
import com.gontry.gestorage.menu.LargeEnderMenu;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderChestBlock.class)
public class EnderChestBlockMixin {
	@Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
	private void gestorage$onUse(net.minecraft.block.BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hitPos, CallbackInfoReturnable<ActionResult> cir) {
		if (world.isClient()) return;

		int sizeMode = ModGameRules.getEnderChestSize(world.getGameRules());
		if (sizeMode == 0) return;

		cir.setReturnValue(ActionResult.success(world.isClient));

		var enderChest = player.getEnderChestInventory();
		if (world.getBlockEntity(pos) instanceof EnderChestBlockEntity blockEntity) {
			enderChest.setActiveBlockEntity(blockEntity);
		}

		int targetSize = sizeMode == 1 ? 54 : 228;
		var stateManager = world.getServer().getOverworld().getPersistentStateManager();
		EnhancedEnderChestInventory enhancedInv = new EnhancedEnderChestInventory(
				enderChest, targetSize, stateManager, player.getUuid()
		);

		if (sizeMode == 1) {
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
		} else if (sizeMode == 2) {
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
	}
}
