package com.gontry.gestorage.mixin;

import com.gontry.gestorage.toolwheel.AutoToolManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void gestorage$onTick(CallbackInfo ci) {
		AutoToolManager.tick((ServerPlayerEntity) (Object) this);
	}
}