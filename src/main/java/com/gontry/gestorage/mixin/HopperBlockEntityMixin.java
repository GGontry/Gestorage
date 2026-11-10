package com.gontry.gestorage.mixin;

import com.gontry.gestorage.shulker.ShulkerStackContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Marks every hopper transfer path as a hopper item-transfer context, so the
 * shulker stack-size mixin can keep empty shulkers at max 1 for hoppers while
 * still allowing players to stack them.
 *
 * <p>Both the block tick and the item-entity collision callback are wrapped, and
 * the context is always cleared in a {@code finally} block so an exception can
 * never leak the flag into the rest of the server tick.
 */
@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {
	@WrapMethod(method = "serverTick")
	private static void gestorage$wrapServerTick(World world, BlockPos pos, BlockState state,
			HopperBlockEntity blockEntity, Operation<Void> original) {
		ShulkerStackContext.enter();
		try {
			original.call(world, pos, state, blockEntity);
		} finally {
			ShulkerStackContext.exit();
		}
	}

	@WrapMethod(method = "onEntityCollided")
	private static void gestorage$wrapOnEntityCollided(World world, BlockPos pos, BlockState state,
			Entity entity, HopperBlockEntity blockEntity, Operation<Void> original) {
		ShulkerStackContext.enter();
		try {
			original.call(world, pos, state, entity, blockEntity);
		} finally {
			ShulkerStackContext.exit();
		}
	}

	/**
	 * Item-entity pickup copies the whole entity stack and lets the destination
	 * cap it, which silently deletes the remainder of a shulker stack that does
	 * not fit. Move at most the current limit and leave the rest in the entity.
	 */
	@Inject(
			method = "extract(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/entity/ItemEntity;)Z",
			at = @At("HEAD"),
			cancellable = true
	)
	private static void gestorage$limitExtractedCount(Inventory inventory, ItemEntity itemEntity,
			CallbackInfoReturnable<Boolean> cir) {
		ItemStack stack = itemEntity.getStack();
		if (stack.isEmpty()) return;

		int limit = stack.getMaxCount();
		if (stack.getCount() <= limit) return;

		ItemStack moving = stack.copyWithCount(limit);
		ItemStack remainder = HopperBlockEntity.transfer(null, inventory, moving, null);
		int moved = limit - remainder.getCount();
		if (moved <= 0) {
			cir.setReturnValue(false);
			return;
		}

		ItemStack left = stack.copy();
		left.decrement(moved);
		itemEntity.setStack(left);
		cir.setReturnValue(true);
	}

	/**
	 * Vanilla merges with {@code stack.getMaxCount() - destination.getCount()}.
	 * With the hopper limit of 1 and a legacy destination stack of 64 that value
	 * is negative, which mutates both stacks into an invalid state. Skip a slot
	 * that is already at (or over) the limit; vanilla would do nothing there.
	 */
	@Inject(
			method = "transfer(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;ILnet/minecraft/util/math/Direction;)Lnet/minecraft/item/ItemStack;",
			at = @At("HEAD"),
			cancellable = true
	)
	private static void gestorage$guardOverMaxMerge(Inventory from, Inventory to, ItemStack stack, int slot,
			Direction side, CallbackInfoReturnable<ItemStack> cir) {
		if (to == null || stack.isEmpty()) return;

		ItemStack destination = to.getStack(slot);
		if (destination.isEmpty()) return;
		if (destination.getCount() < stack.getMaxCount()) return;

		cir.setReturnValue(stack);
	}
}
