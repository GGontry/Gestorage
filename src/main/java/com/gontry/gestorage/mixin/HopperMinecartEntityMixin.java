package com.gontry.gestorage.mixin;

import com.gontry.gestorage.shulker.ShulkerStackContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.vehicle.HopperMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Hopper minecarts call the hopper transfer helpers from their own tick instead
 * of {@code HopperBlockEntity.serverTick}, so they need the same context flag.
 * The flag is cleared in a {@code finally} block and only on the logical server,
 * matching the vanilla client-side early return.
 */
@Mixin(HopperMinecartEntity.class)
public class HopperMinecartEntityMixin {
	@WrapMethod(method = "tick")
	private void gestorage$wrapTick(Operation<Void> original) {
		HopperMinecartEntity self = (HopperMinecartEntity) (Object) this;
		if (self.getEntityWorld().isClient()) {
			original.call();
			return;
		}

		ShulkerStackContext.enter();
		try {
			original.call();
		} finally {
			ShulkerStackContext.exit();
		}
	}
}
