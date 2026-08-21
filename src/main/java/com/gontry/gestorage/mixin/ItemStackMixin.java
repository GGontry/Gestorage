package com.gontry.gestorage.mixin;

import com.gontry.gestorage.config.ShulkerStackServerConfig;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
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
		if (!ShulkerStackServerConfig.enabled) return;

		var item = self.getItem();
		if (!(item instanceof BlockItem blockItem)) return;
		if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) return;

		ContainerComponent container = self.get(DataComponentTypes.CONTAINER);
		if (container != null && hasItems(container)) {
			return;
		}

		cir.setReturnValue(64);
	}

	@Unique
	private static boolean hasItems(ContainerComponent container) {
		for (ItemStack ignored : container.iterateNonEmpty()) {
			return true;
		}
		return false;
	}
}
