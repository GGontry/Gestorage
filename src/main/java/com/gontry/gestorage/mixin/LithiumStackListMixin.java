package com.gontry.gestorage.mixin;

import com.gontry.gestorage.shulker.ShulkerStackRule;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps Lithium's cached full-slot count consistent with the hopper-only
 * shulker stack limit.
 *
 * <p>While a hopper moves an empty shulker, {@link ItemStack#getMaxCount()}
 * reports {@code 1}, so Lithium records the receiving slot as full. When a
 * player later removes that shulker, the stack is evaluated outside the hopper
 * context and reports {@code 64}; Lithium then subtracts nothing from its
 * cached full-slot count. The count never reaches zero, so the hopper believes
 * the destination is still full and never tries to insert again.
 *
 * <p>This mixin makes the full-slot accounting use the same effective limit at
 * insertion and removal time. Lithium updates the count both for whole-slot
 * replacements and for in-place count changes (shift-clicking or taking items
 * by hand), so both paths are covered. It only matters while hopper stacking is
 * disabled; with hopper stacking enabled both sides already use the vanilla
 * limit. The target is marked {@link Pseudo} because Lithium is optional.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.lithium.common.hopper.LithiumStackList")
public class LithiumStackListMixin {
	@Redirect(
			require = 0,
			method = "<init>",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getMaxCount()I")
	)
	private static int gestorage$fullSlotLimitOnLoad(ItemStack stack) {
		return gestorage$fullSlotLimit(stack);
	}

	@Redirect(
			require = 0,
			method = "changedALot",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getMaxCount()I")
	)
	private static int gestorage$fullSlotLimitOnReload(ItemStack stack) {
		return gestorage$fullSlotLimit(stack);
	}

	@Redirect(
			require = 0,
			method = "set",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/item/ItemStack;getMaxCount()I",
					ordinal = 0
			)
	)
	private static int gestorage$fullSlotLimitAfterSet(ItemStack stack) {
		return gestorage$fullSlotLimit(stack);
	}

	@Redirect(
			require = 0,
			method = "set",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/item/ItemStack;getMaxCount()I",
					ordinal = 1
			)
	)
	private static int gestorage$fullSlotLimitBeforeSet(ItemStack stack) {
		return gestorage$fullSlotLimit(stack);
	}

	/**
	 * Covers count changes on an existing stack, such as shift-clicking a stack
	 * out of a chest. Vanilla only calls {@code set} for a whole-slot replacement,
	 * so decrementing a stack in place updates {@code fullSlots} through
	 * {@code lithium$notifyCount} instead.
	 */
	@Redirect(
			require = 0,
			method = "lithium$notifyCount",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getMaxCount()I")
	)
	private static int gestorage$fullSlotLimitOnCountChange(ItemStack stack) {
		return gestorage$fullSlotLimit(stack);
	}

	@Unique
	private static int gestorage$fullSlotLimit(ItemStack stack) {
		if (ShulkerStackRule.hoppersAllowed()) return stack.getMaxCount();
		if (!(stack.getItem() instanceof BlockItem blockItem)) return stack.getMaxCount();
		if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) return stack.getMaxCount();

		ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
		if (container != null) {
			for (ItemStack ignored : container.iterateNonEmpty()) {
				return stack.getMaxCount();
			}
		}

		return 1;
	}
}
