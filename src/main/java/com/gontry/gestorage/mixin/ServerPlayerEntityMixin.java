package com.gontry.gestorage.mixin;

import com.gontry.gestorage.slotlock.SlotLockManager;
import com.gontry.gestorage.toolwheel.AutoToolManager;
import net.minecraft.server.MinecraftServer;
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

	/**
	 * Q outside any screen calls this directly (the THROW slot action is covered by
	 * {@link ScreenHandlerMixin}). Cancel and re-sync the open handler afterwards so
	 * the client does not keep a predicted copy of the dropped item. The method is
	 * {@code void} on 1.21.11, so cancelling it has no return value to restore.
	 */
	@Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
	private void gestorage$preventLockedDrop(boolean entireStack, CallbackInfo ci) {
		ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
		if (!SlotLockManager.shouldPreventDrop(player)) return;

		ci.cancel();
		MinecraftServer server = player.getEntityWorld().getServer();
		if (server != null) {
			server.execute(() -> player.currentScreenHandler.updateToClient());
		}
	}
}
