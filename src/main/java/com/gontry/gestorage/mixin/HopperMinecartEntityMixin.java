package com.gontry.gestorage.mixin;

import com.gontry.gestorage.shulker.ShulkerStackContext;
import net.minecraft.entity.vehicle.HopperMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hopper minecarts call the hopper transfer helpers from their own tick instead
 * of {@code HopperBlockEntity.serverTick}, so they need the same context flag.
 */
@Mixin(HopperMinecartEntity.class)
public class HopperMinecartEntityMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void gestorage$enterTick(CallbackInfo ci) {
		HopperMinecartEntity self = (HopperMinecartEntity) (Object) this;
		if (self.getWorld().isClient()) return;
		ShulkerStackContext.enter();
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void gestorage$exitTick(CallbackInfo ci) {
		HopperMinecartEntity self = (HopperMinecartEntity) (Object) this;
		if (self.getWorld().isClient()) return;
		ShulkerStackContext.exit();
	}
}
