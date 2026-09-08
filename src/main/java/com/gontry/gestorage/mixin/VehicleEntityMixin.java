package com.gontry.gestorage.mixin;

import com.gontry.gestorage.careful.CarefulBreakManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(VehicleEntity.class)
public class VehicleEntityMixin {
	@Inject(method = "damage", at = @At("HEAD"))
	private void gestorage$damageHead(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		VehicleEntity self = (VehicleEntity) (Object) this;
		if (self.getWorld().isClient()) return;
		if (!(source.getAttacker() instanceof ServerPlayerEntity player)) return;
		if (CarefulBreakManager.shouldCollectEntityDrops(player)) {
			CarefulBreakManager.setCurrentEntityKiller(player);
		}
	}

	@Inject(method = "damage", at = @At("RETURN"))
	private void gestorage$damageReturn(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		CarefulBreakManager.clearCurrentEntityKiller();
	}

	@Inject(method = "killAndDropItem", at = @At("HEAD"), cancellable = true)
	private void gestorage$killAndDropItem(Item selfAsItem, CallbackInfo ci) {
		VehicleEntity self = (VehicleEntity) (Object) this;
		if (self.getWorld().isClient()) return;
		ServerPlayerEntity killer = CarefulBreakManager.getCurrentEntityKiller();
		if (killer == null) return;
		if (!CarefulBreakManager.shouldCollectEntityDrops(killer)) return;

		self.kill();
		if (self.getWorld().getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
			ItemStack stack = new ItemStack(selfAsItem);
			stack.set(DataComponentTypes.CUSTOM_NAME, self.getCustomName());
			CarefulBreakManager.collectDrops(killer, List.of(stack));
		}
		ci.cancel();
	}
}