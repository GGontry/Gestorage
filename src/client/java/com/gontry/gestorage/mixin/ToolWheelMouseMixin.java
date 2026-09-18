package com.gontry.gestorage.mixin;

import com.gontry.gestorage.client.ClientToolWheelState;
import com.gontry.gestorage.client.ToolWheelKeybinds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class ToolWheelMouseMixin {
	@Inject(method = "onMouseButton", at = @At("HEAD"))
	private void gestorage$onWheelMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
		if (action != GLFW.GLFW_PRESS) return;
		if (!ClientToolWheelState.wheelActive) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			ClientToolWheelState.wheelClickSuppress = true;
			ToolWheelKeybinds.select(client);
		} else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			ClientToolWheelState.wheelClickSuppress = true;
			ToolWheelKeybinds.deactivate(client);
		}
	}

	@Inject(method = "onMouseButton", at = @At("TAIL"))
	private void gestorage$neutralizeWheelClick(long window, int button, int action, int mods, CallbackInfo ci) {
		if (!ClientToolWheelState.wheelClickSuppress) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			client.options.attackKey.wasPressed();
			client.options.attackKey.setPressed(false);
		} else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			client.options.useKey.wasPressed();
			client.options.useKey.setPressed(false);
		}
		ClientToolWheelState.wheelClickSuppress = false;
	}
}