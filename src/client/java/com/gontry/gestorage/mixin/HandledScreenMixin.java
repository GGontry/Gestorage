package com.gontry.gestorage.mixin;

import com.gontry.gestorage.client.ShulkerRefillRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {
	@Inject(at = @At("TAIL"), method = "drawSlot")
	private void gestorage_onDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
		HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
		ShulkerRefillRenderer.renderSlotBorder(context, slot, screen);
	}
}
