package com.gontry.gestorage.mixin;

import com.gontry.gestorage.client.InventorySortingRenderer;
import com.gontry.gestorage.client.ShulkerRefillRenderer;
import com.gontry.gestorage.client.SlotLockRenderer;
import com.gontry.gestorage.client.StorageOverlayRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {
	@Inject(at = @At("TAIL"), method = "drawSlot")
	private void gestorage_onDrawSlot(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
		ShulkerRefillRenderer.renderSlotBorder(context, slot, screen);
		SlotLockRenderer.renderSlotBorder(context, slot, screen);
	}

	@Inject(at = @At("HEAD"), method = "render")
	private void gestorage_beforeRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		SlotLockRenderer.beginFrame();
	}

	@Inject(at = @At("TAIL"), method = "render")
	private void gestorage_onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
		StorageOverlayRenderer.render(context, screen, mouseX, mouseY);
		InventorySortingRenderer.renderButton(context, screen, mouseX, mouseY);
	}

	@Inject(at = @At("HEAD"), method = "mouseClicked", cancellable = true)
	private void gestorage_onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
		if (click.button() != 0) return;
		HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
		if (SlotLockRenderer.handleClick(screen, click.x(), click.y())) {
			cir.setReturnValue(true);
			return;
		}
		if (InventorySortingRenderer.handleClick(screen, (int) click.x(), (int) click.y())) {
			cir.setReturnValue(true);
		}
	}
}
