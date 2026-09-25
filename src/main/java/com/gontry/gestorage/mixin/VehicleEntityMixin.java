package com.gontry.gestorage.mixin;

import com.gontry.gestorage.careful.CarefulBreakManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VehicleEntity.class)
public class VehicleEntityMixin {
	@Inject(method = "damage", at = @At("HEAD"))
	private void gestorage$damageHead(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		VehicleEntity self = (VehicleEntity) (Object) this;
		if (self.getWorld().isClient()) return;
		if (source.getAttacker() instanceof ServerPlayerEntity player) {
			CarefulBreakManager.beginDeath(player, self);
		}
	}

	@Inject(method = "damage", at = @At("RETURN"))
	private void gestorage$damageReturn(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		CarefulBreakManager.endDeath();
	}
}