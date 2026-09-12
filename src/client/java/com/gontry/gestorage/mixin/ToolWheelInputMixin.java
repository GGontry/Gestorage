package com.gontry.gestorage.mixin;

import com.gontry.gestorage.client.ClientToolWheelState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftClient.class)
public class ToolWheelInputMixin {
	@Redirect(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z"))
	private boolean gestorage$blockPressedWhileWheelOpen(KeyBinding instance) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (ClientToolWheelState.wheelActive) {
			if (instance == client.options.attackKey || instance == client.options.useKey || instance == client.options.pickItemKey) {
				instance.wasPressed();
				return false;
			}
		}
		return instance.wasPressed();
	}

	@Redirect(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
	private boolean gestorage$blockHeldWhileWheelOpen(KeyBinding instance) {
		if (ClientToolWheelState.wheelActive) {
			MinecraftClient client = MinecraftClient.getInstance();
			if (instance == client.options.attackKey || instance == client.options.useKey) {
				return false;
			}
		}
		return instance.isPressed();
	}
}