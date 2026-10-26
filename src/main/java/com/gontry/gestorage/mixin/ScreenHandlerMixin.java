package com.gontry.gestorage.mixin;

import com.gontry.gestorage.slotlock.SlotLockManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents Q / drag-out drops on locked slots. The click is cancelled before
 * vanilla mutates the slot, and the slot is re-synced after the packet handler
 * finishes so the client never keeps a ghost copy of the item.
 */
@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {
	@Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
	private void gestorage$preventLockedThrow(int slotIndex, int button, SlotActionType actionType,
			PlayerEntity player, CallbackInfo ci) {
		if (actionType != SlotActionType.THROW) return;
		if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

		ScreenHandler self = (ScreenHandler) (Object) this;
		if (self != serverPlayer.currentScreenHandler) return;
		if (slotIndex < 0 || slotIndex >= self.slots.size()) return;

		Slot slot = self.slots.get(slotIndex);
		if (!SlotLockManager.shouldPreventDrop(serverPlayer, slot)) return;

		ci.cancel();
		MinecraftServer server = serverPlayer.getServer();
		if (server != null) {
			server.execute(() -> serverPlayer.currentScreenHandler.updateToClient());
		}
	}
}
