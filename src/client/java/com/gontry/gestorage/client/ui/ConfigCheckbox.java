package com.gontry.gestorage.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ConfigCheckbox extends PressableWidget {
	private final BooleanSupplier getter;
	private final Consumer<Boolean> setter;
	private final Runnable onToggle;
	private boolean selected;

	public ConfigCheckbox(int x, int y, int width, int height, Text label,
			BooleanSupplier getter, Consumer<Boolean> setter, Runnable onToggle) {
		super(x, y, width, height, label);
		this.getter = getter;
		this.setter = setter;
		this.onToggle = onToggle;
		this.selected = getter.getAsBoolean();
	}

	@Override
	public void onPress() {
		this.selected = !this.selected;
		this.setter.accept(this.selected);
		if (this.onToggle != null) {
			this.onToggle.run();
		}
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		this.appendDefaultNarrations(builder);
	}

	@Override
	protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
		int iconY = this.getY() + (this.height - 12) / 2;
		RenderSystem.setShaderColor(1f, 1f, 1f, this.active ? 1f : 0.4f);
		ConfigTextures.drawCheckbox(context, this.selected, this.getX(), iconY);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

		int color = !this.active ? 0xFF8A8A8A : (this.isHovered() ? 0xFFFFFFFF : 0xFFD9D9D9);
		context.drawText(MinecraftClient.getInstance().textRenderer, this.getMessage(),
				this.getX() + 18, this.getY() + (this.height - 8) / 2, color, false);
	}
}
