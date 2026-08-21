package com.gontry.gestorage.mixin;

import com.gontry.gestorage.careful.CarefulBreakManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Block.class)
public class BlockDropMixin {
	@Inject(method = "dropStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)V",
			at = @At("HEAD"), cancellable = true)
	private static void gestorage$dropStacks(BlockState state, World world, BlockPos pos,
			BlockEntity blockEntity, Entity entity, ItemStack tool, CallbackInfo ci) {
		if (!(entity instanceof ServerPlayerEntity player)) return;
		if (!CarefulBreakManager.shouldCollectBlockDrops(player)) return;

		List<ItemStack> drops = Block.getDroppedStacks(state, (ServerWorld) world, pos, blockEntity, entity, tool);
		CarefulBreakManager.collectDrops(player, drops, world, pos);
		state.updateNeighbors(world, pos, 3);
		ci.cancel();
	}
}
