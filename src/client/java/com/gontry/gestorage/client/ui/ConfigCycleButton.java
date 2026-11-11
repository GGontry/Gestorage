package com.gontry.gestorage.client.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.function.Supplier;

/**
 * Button whose caption is refreshed every frame from a supplier, so an option whose
 * value is mirrored from the server (or cycles through several states) always shows
 * the current state without rebuilding the screen.
 */
public class ConfigCycleButton extends ConfigButton {
	private final Supplier<Text> labelSupplier;

	public ConfigCycleButton(int x, int y, int width, int height, Supplier<Text> labelSupplier, Runnable onCycle) {
		super(x, y, width, height, labelSupplier.get(), onCycle::run);
		this.labelSupplier = labelSupplier;
	}

	@Override
	protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		Text label = labelSupplier.get();
		if (!label.getString().equals(getMessage().getString())) {
			setMessage(label);
		}
		super.drawIcon(context, mouseX, mouseY, deltaTicks);
	}
}
