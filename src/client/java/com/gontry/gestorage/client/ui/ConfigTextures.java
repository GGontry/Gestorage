package com.gontry.gestorage.client.ui;

import com.gontry.gestorage.Gestorage;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

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

	/** Draws the texture at full opacity; see {@link #drawNineSlice(DrawContext, Identifier, int, int, int, int, int)}. */
	public static void drawNineSlice(DrawContext context, Identifier texture, int x, int y, int width, int height) {
		drawNineSlice(context, texture, x, y, width, height, -1);
	}

	/**
	 * Draws a nine-sliced panel from a 16x16 texture with a 1px border. {@code color}
	 * tints the whole panel; use {@link ColorHelper#getWhite(float)} to dim inactive widgets.
	 */
	public static void drawNineSlice(DrawContext context, Identifier texture, int x, int y, int width, int height, int color) {
		int right = x + width - BORDER;
		int bottom = y + height - BORDER;
		int innerWidth = Math.max(width - BORDER * 2, 0);
		int innerHeight = Math.max(height - BORDER * 2, 0);

		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, BORDER, BORDER, BORDER, BORDER, TEXTURE_SIZE, TEXTURE_SIZE, color);
		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x + BORDER, y, BORDER, 0.0F, innerWidth, BORDER, TEXTURE_SIZE - BORDER * 2, BORDER, TEXTURE_SIZE, TEXTURE_SIZE, color);
		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, right, y, TEXTURE_SIZE - BORDER, 0.0F, BORDER, BORDER, BORDER, BORDER, TEXTURE_SIZE, TEXTURE_SIZE, color);

		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y + BORDER, 0.0F, BORDER, BORDER, innerHeight, BORDER, TEXTURE_SIZE - BORDER * 2, TEXTURE_SIZE, TEXTURE_SIZE, color);
		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x + BORDER, y + BORDER, BORDER, BORDER, innerWidth, innerHeight, TEXTURE_SIZE - BORDER * 2, TEXTURE_SIZE - BORDER * 2, TEXTURE_SIZE, TEXTURE_SIZE, color);
		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, right, y + BORDER, TEXTURE_SIZE - BORDER, BORDER, BORDER, innerHeight, BORDER, TEXTURE_SIZE - BORDER * 2, TEXTURE_SIZE, TEXTURE_SIZE, color);

		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, bottom, 0.0F, TEXTURE_SIZE - BORDER, BORDER, BORDER, BORDER, BORDER, TEXTURE_SIZE, TEXTURE_SIZE, color);
		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x + BORDER, bottom, BORDER, TEXTURE_SIZE - BORDER, innerWidth, BORDER, TEXTURE_SIZE - BORDER * 2, BORDER, TEXTURE_SIZE, TEXTURE_SIZE, color);
		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, right, bottom, TEXTURE_SIZE - BORDER, TEXTURE_SIZE - BORDER, BORDER, BORDER, BORDER, BORDER, TEXTURE_SIZE, TEXTURE_SIZE, color);
	}

	/** Draws the checkbox at full opacity; see {@link #drawCheckbox(DrawContext, boolean, int, int, int)}. */
	public static void drawCheckbox(DrawContext context, boolean on, int x, int y) {
		drawCheckbox(context, on, x, y, -1);
	}

	/** Draws the 12x12 checkbox texture; {@code color} tints it, see {@link #drawNineSlice}. */
	public static void drawCheckbox(DrawContext context, boolean on, int x, int y, int color) {
		context.drawTexture(RenderPipelines.GUI_TEXTURED, on ? CHECKBOX_ON : CHECKBOX_OFF, x, y, 0.0F, 0.0F, 12, 12, 12, 12, color);
	}
}
