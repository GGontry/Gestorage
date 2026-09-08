package com.gontry.gestorage.mixin;

import com.gontry.gestorage.careful.CarefulBreakManager;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ItemScatterer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemScatterer.class)
public class ItemScattererMixin {
	@Inject(method = "spawn(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/inventory/Inventory;)V",
			at = @At("HEAD"), cancellable = true)
	private static void gestorage$spawn(World world, Entity entity, Inventory inventory, CallbackInfo ci) {
		if (world.isClient()) return;
		ServerPlayerEntity killer = CarefulBreakManager.getCurrentEntityKiller();
		if (killer == null) return;
		if (!CarefulBreakManager.shouldCollectEntityDrops(killer)) return;

		List<ItemStack> drops = new ArrayList<>();
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (!stack.isEmpty()) {
				drops.add(stack.copy());
				inventory.setStack(i, ItemStack.EMPTY);
			}
		}
		CarefulBreakManager.collectDrops(killer, drops);
		ci.cancel();
	}
}