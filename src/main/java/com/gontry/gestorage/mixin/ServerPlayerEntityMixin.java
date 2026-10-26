package com.gontry.gestorage.mixin;

import com.gontry.gestorage.slotlock.SlotLockManager;
import com.gontry.gestorage.toolwheel.AutoToolManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void gestorage$onTick(CallbackInfo ci) {
		AutoToolManager.tick((ServerPlayerEntity) (Object) this);
	}

	/**
	 * Q outside any screen calls this directly (the THROW slot action is covered by
	 * {@link ScreenHandlerMixin}). Cancel and re-sync the open handler afterwards so
	 * the client does not keep a predicted copy of the dropped item.
	 */
	@Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
	private void gestorage$preventLockedDrop(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
		ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
		if (!SlotLockManager.shouldPreventDrop(player)) return;

		cir.setReturnValue(false);
		MinecraftServer server = player.getServer();
		if (server != null) {
			server.execute(() -> player.currentScreenHandler.updateToClient());
		}
	}
}
