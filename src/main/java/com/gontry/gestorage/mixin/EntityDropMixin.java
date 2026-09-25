package com.gontry.gestorage.mixin;

import com.gontry.gestorage.careful.CarefulBreakManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class EntityDropMixin {
	@Inject(method = "onDeath", at = @At("HEAD"))
	private void gestorage$onDeath(DamageSource source, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.getWorld().isClient()) return;
		if (source.getAttacker() instanceof ServerPlayerEntity player) {
			CarefulBreakManager.beginDeath(player, self);
		}
	}

	@Inject(method = "onDeath", at = @At("RETURN"))
	private void gestorage$onDeathReturn(DamageSource source, CallbackInfo ci) {
		CarefulBreakManager.endDeath();
	}
}