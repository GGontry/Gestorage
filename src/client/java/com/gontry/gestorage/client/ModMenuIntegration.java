package com.gontry.gestorage.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return this::createConfigScreen;
	}

	private Screen createConfigScreen(Screen parent) {
		return new ConfigScreen(parent);
	}

	private static class ConfigScreen extends Screen {
		private final Screen parent;
		private ButtonWidget openEnderKeyButton;
		private ButtonWidget shulkerRefillKeyButton;
		private int waitingForKey = -1;
		private int capturedMods = 0;
		private boolean modMenuWasActive = false;

		protected ConfigScreen(Screen parent) {
			super(Text.translatable("gestorage.config.title"));
			this.parent = parent;
		}

		@Override
		protected void init() {
			openEnderKeyButton = ButtonWidget.builder(
					Text.literal("Open Ender Chest: ").append(KeybindHelper.getKeyName(ModConfig.get().getOpenEnderChestKey())),
					button -> {
						waitingForKey = 0;
						capturedMods = 0;
						modMenuWasActive = false;
						button.setMessage(Text.literal("Open Ender Chest: ").append(Text.translatable("gestorage.config.press_key")));
					}
			).dimensions(this.width / 2 - 100, this.height / 2 - 30, 200, 20).build();

			shulkerRefillKeyButton = ButtonWidget.builder(
					Text.literal("Shulker Refill: ").append(KeybindHelper.getKeyName(ModConfig.get().getShulkerRefillKey())),
					button -> {
						waitingForKey = 1;
						capturedMods = 0;
						modMenuWasActive = false;
						button.setMessage(Text.literal("Shulker Refill: ").append(Text.translatable("gestorage.config.press_key")));
					}
			).dimensions(this.width / 2 - 100, this.height / 2, 200, 20).build();

			this.addDrawableChild(openEnderKeyButton);
			this.addDrawableChild(shulkerRefillKeyButton);

			this.addDrawableChild(ButtonWidget.builder(
					Text.translatable("gestorage.config.done"),
					button -> this.client.setScreen(parent)
			).dimensions(this.width / 2 - 100, this.height / 2 + 40, 200, 20).build());
		}

		@Override
		public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if (waitingForKey != -1) {
				if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
					setKeybind(waitingForKey, "");
					waitingForKey = -1;
					return true;
				}

				if (isModifier(keyCode)) {
					updateCapturedMods(keyCode);
					return true;
				}

				setKeybind(waitingForKey, KeybindHelper.encode(keyCode, capturedMods));
				waitingForKey = -1;
				return true;
			}
			return super.keyPressed(keyCode, scanCode, modifiers);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (waitingForKey != -1) {
				int mouseCode = -button;
				setKeybind(waitingForKey, KeybindHelper.encode(mouseCode, capturedMods));
				waitingForKey = -1;
				return true;
			}
			return super.mouseClicked(mouseX, mouseY, button);
		}

		private void setKeybind(int slot, String value) {
			if (slot == 0) {
				ModConfig.get().setOpenEnderChestKey(value);
				openEnderKeyButton.setMessage(Text.literal("Open Ender Chest: ").append(KeybindHelper.getKeyName(value)));
			} else if (slot == 1) {
				ModConfig.get().setShulkerRefillKey(value);
				shulkerRefillKeyButton.setMessage(Text.literal("Shulker Refill: ").append(KeybindHelper.getKeyName(value)));
			}
			ModConfig.save();
		}

		private boolean isModifier(int keyCode) {
			return keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL ||
				   keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT ||
				   keyCode == GLFW.GLFW_KEY_LEFT_ALT || keyCode == GLFW.GLFW_KEY_RIGHT_ALT ||
				   keyCode == GLFW.GLFW_KEY_LEFT_SUPER || keyCode == GLFW.GLFW_KEY_RIGHT_SUPER;
		}

		private void updateCapturedMods(int keyCode) {
			switch (keyCode) {
				case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> capturedMods |= KeybindHelper.MOD_CTRL;
				case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> capturedMods |= KeybindHelper.MOD_SHIFT;
				case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> capturedMods |= KeybindHelper.MOD_ALT;
				case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> capturedMods |= KeybindHelper.MOD_SUPER;
			}
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, float delta) {
			super.render(context, mouseX, mouseY, delta);
			context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
			context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("gestorage.config.info"), this.width / 2, 40, 0xAAAAAA);
		}

		@Override
		public void close() {
			this.client.setScreen(parent);
		}
	}
}
