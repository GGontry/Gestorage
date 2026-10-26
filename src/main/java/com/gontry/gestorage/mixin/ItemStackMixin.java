package com.gontry.gestorage.mixin;

import com.gontry.gestorage.shulker.ShulkerStackContext;
import com.gontry.gestorage.shulker.ShulkerStackRule;
import com.gontry.gestorage.slotlock.SlotLockManager;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {

	@Inject(method = "getMaxCount", at = @At("HEAD"), cancellable = true)
	private void gestorage$getMaxCount(CallbackInfoReturnable<Integer> cir) {
		ItemStack self = (ItemStack) (Object) this;

		var item = self.getItem();
		if (!(item instanceof BlockItem blockItem)) return;
		if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) return;

		if (!ShulkerStackRule.enabled()) return;

		ContainerComponent container = self.get(DataComponentTypes.CONTAINER);
		if (container != null && hasItems(container)) {
			return;
		}

		// Hoppers only stack empty shulkers when the dedicated rule allows it.
		if (ShulkerStackContext.active() && !ShulkerStackRule.hoppersAllowed()) {
			return;
		}

		cir.setReturnValue(64);
	}

	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void gestorage$preventLockedUse(World world, PlayerEntity user, Hand hand,
			CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
		if (world.isClient()) return;
		if (!(user instanceof ServerPlayerEntity player)) return;
		if (!SlotLockManager.shouldPreventUse(player, hand)) return;
		cir.setReturnValue(TypedActionResult.pass((ItemStack) (Object) this));
	}

	@Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
	private void gestorage$preventLockedUseOnBlock(ItemUsageContext context,
			CallbackInfoReturnable<ActionResult> cir) {
		if (context.getWorld().isClient()) return;
		PlayerEntity user = context.getPlayer();
		if (!(user instanceof ServerPlayerEntity player)) return;
		if (!SlotLockManager.shouldPreventUse(player, context.getHand())) return;
		cir.setReturnValue(ActionResult.PASS);
	}

	@Inject(method = "useOnEntity", at = @At("HEAD"), cancellable = true)
	private void gestorage$preventLockedUseOnEntity(PlayerEntity user, LivingEntity entity, Hand hand,
			CallbackInfoReturnable<ActionResult> cir) {
		if (user.getWorld().isClient()) return;
		if (!(user instanceof ServerPlayerEntity player)) return;
		if (!SlotLockManager.shouldPreventUse(player, hand)) return;
		cir.setReturnValue(ActionResult.PASS);
	}

	@Unique
	private static boolean hasItems(ContainerComponent container) {
		for (ItemStack ignored : container.iterateNonEmpty()) {
			return true;
		}
		return false;
	}
}
