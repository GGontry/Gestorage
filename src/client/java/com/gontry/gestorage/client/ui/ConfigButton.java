package com.gontry.gestorage.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ConfigButton extends PressableWidget {
	private final Runnable onPressAction;
	private boolean selected;

	public ConfigButton(int x, int y, int width, int height, Text message, Runnable onPress) {
		super(x, y, width, height, message);
		this.onPressAction = onPress;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isSelected() {
		return selected;
	}

	@Override
	public void onPress() {
		if (onPressAction != null) {
			onPressAction.run();
		}
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		this.appendDefaultNarrations(builder);
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		Identifier texture = ConfigTextures.BUTTON;
		if (!this.active) {
			texture = ConfigTextures.BUTTON;
		} else if (this.selected) {
			texture = ConfigTextures.BUTTON_SELECTED;
		} else if (this.isPressed()) {
			texture = ConfigTextures.BUTTON_PRESSED;
		} else if (this.isHovered()) {
			texture = ConfigTextures.BUTTON_HOVER;
		}

		RenderSystem.setShaderColor(1f, 1f, 1f, this.active ? 1f : 0.4f);
		ConfigTextures.drawNineSlice(context, texture, this.getX(), this.getY(), this.width, this.height);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

		int color = !this.active ? 0xFF8A8A8A : (this.isHovered() || this.selected ? 0xFFFFFFFF : 0xFFD9D9D9);
		this.drawMessage(context, MinecraftClient.getInstance().textRenderer, color);
	}

	private boolean isPressed() {
		long handle = MinecraftClient.getInstance().getWindow().getHandle();
		return this.isHovered() && GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
	}
}
