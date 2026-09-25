package com.gontry.gestorage.mixin;

import com.gontry.gestorage.careful.CarefulBreakManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityDropStackMixin {
	@Inject(method = "dropStack(Lnet/minecraft/item/ItemStack;F)Lnet/minecraft/entity/ItemEntity;",
			at = @At("HEAD"), cancellable = true)
	private void gestorage$dropStack(ItemStack stack, float yOffset, CallbackInfoReturnable<ItemEntity> cir) {
		if (stack.isEmpty()) return;
		Entity self = (Entity) (Object) this;
		if (self.getWorld().isClient()) return;
		CarefulBreakManager.DeathContext ctx = CarefulBreakManager.getCurrentDeath();
		if (ctx == null) return;
		if ((Object) this != ctx.victim()) return;
		ServerPlayerEntity killer = ctx.killer();
		if (!CarefulBreakManager.shouldCollectEntityDrops(killer)) return;
		cir.setReturnValue(CarefulBreakManager.collectOrSpawn(killer, stack));
	}
}