package com.gontry.gestorage.client.ui;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

public class ConfigIconButton extends PressableWidget {
	private final Runnable onPressAction;
	private final Identifier texture;
	private final Identifier hoverTexture;

	public ConfigIconButton(int x, int y, int width, int height, Identifier texture, Identifier hoverTexture, Runnable onPress) {
		super(x, y, width, height, Text.literal("Close"));
		this.texture = texture;
		this.hoverTexture = hoverTexture;
		this.onPressAction = onPress;
	}

	@Override
	public void onPress(AbstractInput input) {
		if (onPressAction != null) {
			onPressAction.run();
		}
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		this.appendDefaultNarrations(builder);
	}

	@Override
	protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		Identifier tex = this.isHovered() ? hoverTexture : texture;
		context.drawTexture(RenderPipelines.GUI_TEXTURED, tex, this.getX(), this.getY(), 0.0F, 0.0F,
				this.width, this.height, this.width, this.height, ColorHelper.getWhite(this.active ? 1.0F : 0.4F));
	}
}
