package com.gontry.gestorage.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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
		Identifier tex = this.isHovered() ? hoverTexture : texture;
		RenderSystem.setShaderColor(1f, 1f, 1f, this.active ? 1f : 0.4f);
		context.drawTexture(tex, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
	}
}
