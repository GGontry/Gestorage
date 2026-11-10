package com.gontry.gestorage.mixin;

import com.gontry.gestorage.shulker.ShulkerStackHelper;
import com.gontry.gestorage.shulker.ShulkerStackRule;
import net.minecraft.block.entity.Hopper;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Caps GUI insertion into hopper slots while hopper stacking is off.
 *
 * <p>Hopper transfers run inside {@link com.gontry.gestorage.shulker.ShulkerStackContext},
 * but a shift-click or a manual click in the hopper screen does not. Without this
 * limit the client and server would happily move a whole 64-shulker stack into a
 * hopper slot, bypassing the rule the hopper itself follows.
 */
@Mixin(Slot.class)
public class SlotMixin {
	@Inject(method = "getMaxItemCount(Lnet/minecraft/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
	private void gestorage$limitHopperSlot(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
		Slot self = (Slot) (Object) this;
		if (!(self.inventory instanceof Hopper)) return;
		if (ShulkerStackRule.hoppersAllowed()) return;
		if (!ShulkerStackHelper.isEmptyShulker(stack)) return;

		cir.setReturnValue(Math.min(self.getMaxItemCount(), 1));
	}
}
