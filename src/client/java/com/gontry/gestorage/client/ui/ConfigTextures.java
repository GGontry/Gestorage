package com.gontry.gestorage.client.ui;

import com.gontry.gestorage.Gestorage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class ConfigTextures {
	public static final Identifier WINDOW = Identifier.of(Gestorage.MOD_ID, "textures/gui/config/window.png");
	public static final Identifier BUTTON = Identifier.of(Gestorage.MOD_ID, "textures/gui/config/button.png");
	public static final Identifier BUTTON_HOVER = Identifier.of(Gestorage.MOD_ID, "textures/gui/config/button_hover.png");
	public static final Identifier BUTTON_PRESSED = Identifier.of(Gestorage.MOD_ID, "textures/gui/config/button_pressed.png");
	public static final Identifier BUTTON_SELECTED = Identifier.of(Gestorage.MOD_ID, "textures/gui/config/button_selected.png");
	public static final Identifier CHECKBOX_ON = Identifier.of(Gestorage.MOD_ID, "textures/gui/config/checkbox_on.png");
	public static final Identifier CHECKBOX_OFF = Identifier.of(Gestorage.MOD_ID, "textures/gui/config/checkbox_off.png");
	public static final Identifier CLOSE = Identifier.of(Gestorage.MOD_ID, "textures/gui/config/close.png");
	public static final Identifier CLOSE_HOVER = Identifier.of(Gestorage.MOD_ID, "textures/gui/config/close_hover.png");

	private static final int TEXTURE_SIZE = 16;
	private static final int BORDER = 1;

	private ConfigTextures() {}

	public static void drawNineSlice(DrawContext context, Identifier texture, int x, int y, int width, int height) {
		int right = x + width - BORDER;
		int bottom = y + height - BORDER;
		int innerWidth = Math.max(width - BORDER * 2, 0);
		int innerHeight = Math.max(height - BORDER * 2, 0);

		context.drawTexture(texture, x, y, BORDER, BORDER, 0, 0, BORDER, BORDER, TEXTURE_SIZE, TEXTURE_SIZE);
		context.drawTexture(texture, x + BORDER, y, innerWidth, BORDER, BORDER, 0, TEXTURE_SIZE - BORDER * 2, BORDER, TEXTURE_SIZE, TEXTURE_SIZE);
		context.drawTexture(texture, right, y, BORDER, BORDER, TEXTURE_SIZE - BORDER, 0, BORDER, BORDER, TEXTURE_SIZE, TEXTURE_SIZE);

		context.drawTexture(texture, x, y + BORDER, BORDER, innerHeight, 0, BORDER, BORDER, TEXTURE_SIZE - BORDER * 2, TEXTURE_SIZE, TEXTURE_SIZE);
		context.drawTexture(texture, x + BORDER, y + BORDER, innerWidth, innerHeight, BORDER, BORDER, TEXTURE_SIZE - BORDER * 2, TEXTURE_SIZE - BORDER * 2, TEXTURE_SIZE, TEXTURE_SIZE);
		context.drawTexture(texture, right, y + BORDER, BORDER, innerHeight, TEXTURE_SIZE - BORDER, BORDER, BORDER, TEXTURE_SIZE - BORDER * 2, TEXTURE_SIZE, TEXTURE_SIZE);

		context.drawTexture(texture, x, bottom, BORDER, BORDER, 0, TEXTURE_SIZE - BORDER, BORDER, BORDER, TEXTURE_SIZE, TEXTURE_SIZE);
		context.drawTexture(texture, x + BORDER, bottom, innerWidth, BORDER, BORDER, TEXTURE_SIZE - BORDER, TEXTURE_SIZE - BORDER * 2, BORDER, TEXTURE_SIZE, TEXTURE_SIZE);
		context.drawTexture(texture, right, bottom, BORDER, BORDER, TEXTURE_SIZE - BORDER, TEXTURE_SIZE - BORDER, BORDER, BORDER, TEXTURE_SIZE, TEXTURE_SIZE);
	}

	public static void drawCheckbox(DrawContext context, boolean on, int x, int y) {
		context.drawTexture(on ? CHECKBOX_ON : CHECKBOX_OFF, x, y, 0, 0, 12, 12, 12, 12);
	}
}
