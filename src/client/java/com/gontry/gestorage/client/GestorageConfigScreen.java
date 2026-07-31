package com.gontry.gestorage.client;

import com.gontry.gestorage.client.config.ModuleConfig;
import com.gontry.gestorage.config.ShulkerStackServerConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class GestorageConfigScreen extends Screen {
	private final Screen parent;
	private int activeTab = 0;
	private int selectedModule = 0;
	private boolean waitingForKey = false;
	private int keybindTarget = -1;
	private int capturedMods = 0;
	private ButtonWidget waitingButton = null;
	private String searchText = "";
	private String originalSearchText = "";

	private TextFieldWidget searchField;
	private ButtonWidget clearBtn;
	private ButtonWidget modulesTab;
	private ButtonWidget keybindsTab;
	private ButtonWidget settingsTab;
	private final List<ButtonWidget> moduleButtons = new ArrayList<>();
	private ButtonWidget enderEnabledBtn;
	private ButtonWidget enderKeyBtn;
	private ButtonWidget shulkerEnabledBtn;
	private ButtonWidget shulkerKeyBtn;
	private ButtonWidget enderKeyKeybindBtn;
	private ButtonWidget shulkerKeyKeybindBtn;
	private ButtonWidget stackEnabledBtn;

	private static final int TAB_WIDTH = 70;
	private static final int LEFT_WIDTH = 130;
	private static final int TAB_Y = 4;
	private static final int SEARCH_Y = 28;
	private static final int CONTENT_Y = 52;

	public GestorageConfigScreen(Screen parent) {
		super(Text.literal("Gestorage Settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		buildSearchField();
		buildTabs();
		buildContent();
		searchField.setFocused(true);
		this.setFocused(searchField);
	}

	private void buildSearchField() {
		int fieldW = Math.min(300, this.width - 80);
		int fieldX = (this.width - fieldW) / 2;
		int btnX = fieldX + fieldW;

		searchField = new TextFieldWidget(this.textRenderer, fieldX, SEARCH_Y, fieldW, 18, Text.literal(""));
		searchField.setPlaceholder(Text.literal("Search modules or keybinds..."));
		searchField.setEditableColor(0xFFFFFFFF);
		searchField.setUneditableColor(0xFF808080);
		searchField.setDrawsBackground(true);
		searchField.setText(originalSearchText);
		searchField.setMaxLength(64);
		searchField.setChangedListener(text -> {
			originalSearchText = text;
			searchText = text.toLowerCase();
			onSearchChanged();
		});
		addDrawableChild(searchField);

		clearBtn = ButtonWidget.builder(Text.literal("X"), b -> {
			searchField.setText("");
			searchField.setFocused(true);
			this.setFocused(searchField);
		}).dimensions(btnX + 2, SEARCH_Y, 18, 18).build();
		addDrawableChild(clearBtn);
	}

	private void onSearchChanged() {
		selectedModule = findFirstVisibleModule(0);
		removeContentWidgets();
		buildContent();
	}

	private void buildTabs() {
		int totalTabsWidth = TAB_WIDTH * 3 + 4;
		int startX = (this.width - totalTabsWidth) / 2;

		modulesTab = ButtonWidget.builder(Text.literal("Modules"), b -> setTab(0))
				.dimensions(startX, TAB_Y, TAB_WIDTH, 20).build();
		keybindsTab = ButtonWidget.builder(Text.literal("Keybinds"), b -> setTab(1))
				.dimensions(startX + TAB_WIDTH + 2, TAB_Y, TAB_WIDTH, 20).build();
		settingsTab = ButtonWidget.builder(Text.literal("Settings"), b -> setTab(2))
				.dimensions(startX + (TAB_WIDTH + 2) * 2, TAB_Y, TAB_WIDTH, 20).build();

		addDrawableChild(modulesTab);
		addDrawableChild(keybindsTab);
		addDrawableChild(settingsTab);
	}

	private void setTab(int tab) {
		activeTab = tab;
		waitingForKey = false;
		keybindTarget = -1;
		waitingButton = null;
		fullRebuild();
	}

	private void fullRebuild() {
		clearChildren();
		buildSearchField();
		buildTabs();
		buildContent();
	}

	private void buildContent() {
		switch (activeTab) {
			case 0 -> buildModulesTab();
			case 1 -> buildKeybindsTab();
			case 2 -> buildSettingsTab();
		}
	}

	private void removeContentWidgets() {
		for (ButtonWidget btn : moduleButtons) {
			remove(btn);
		}
		moduleButtons.clear();
		if (enderEnabledBtn != null) { remove(enderEnabledBtn); enderEnabledBtn = null; }
		if (enderKeyBtn != null) { remove(enderKeyBtn); enderKeyBtn = null; }
		if (shulkerEnabledBtn != null) { remove(shulkerEnabledBtn); shulkerEnabledBtn = null; }
		if (shulkerKeyBtn != null) { remove(shulkerKeyBtn); shulkerKeyBtn = null; }
		if (enderKeyKeybindBtn != null) { remove(enderKeyKeybindBtn); enderKeyKeybindBtn = null; }
		if (shulkerKeyKeybindBtn != null) { remove(shulkerKeyKeybindBtn); shulkerKeyKeybindBtn = null; }
		if (stackEnabledBtn != null) { remove(stackEnabledBtn); stackEnabledBtn = null; }

	}

	private boolean moduleMatchesSearch(int idx) {
		if (searchText.isEmpty()) return true;
		String title = getModuleTitle(idx).toLowerCase();
		String desc = getModuleDesc(idx).toLowerCase();
		return title.contains(searchText) || desc.contains(searchText);
	}

	private int findFirstVisibleModule(int startFrom) {
		for (int i = startFrom; i < 3; i++) {
			if (moduleMatchesSearch(i)) return i;
		}
		return -1;
	}

	private void buildModulesTab() {
		int contentY = CONTENT_Y;
		int leftX = 10;
		int panelWidth = this.width - 20;
		int rightX = leftX + LEFT_WIDTH + 8;

		moduleButtons.clear();
		int visibleIdx = 0;
		for (int i = 0; i < 3; i++) {
			if (!moduleMatchesSearch(i)) continue;
			int idx = i;
			String label = (i == selectedModule ? "> " : "  ") + getModuleTitle(i);
			ButtonWidget btn = ButtonWidget.builder(
					Text.literal(label),
					b -> selectModule(idx)
			).dimensions(leftX, contentY + visibleIdx * 22, LEFT_WIDTH, 20).build();
			moduleButtons.add(btn);
			addDrawableChild(btn);
			visibleIdx++;
		}

		if (selectedModule >= 0 && selectedModule < 3 && moduleMatchesSearch(selectedModule)) {
			if (selectedModule == 0) {
				int optY = contentY + 68;
				enderEnabledBtn = ButtonWidget.builder(
						Text.literal("Enabled: " + (ModuleConfig.enderChest().enabled() ? "ON" : "OFF")),
						b -> {
							boolean now = !ModuleConfig.enderChest().enabled();
							ModuleConfig.enderChest().enabled(now);
							ModuleConfig.enderChest().save();
							b.setMessage(Text.literal("Enabled: " + (now ? "ON" : "OFF")));
						}
				).dimensions(rightX, optY, 120, 20).build();

				enderKeyBtn = ButtonWidget.builder(
						Text.literal("Key: " + KeybindHelper.getKeyName(ModuleConfig.enderChest().openEnderChestKey())),
						b -> startKeybindCapture(0, b)
				).dimensions(rightX + 125, optY, 120, 20).build();

				addDrawableChild(enderEnabledBtn);
				addDrawableChild(enderKeyBtn);
			} else if (selectedModule == 1) {
				int optY = contentY + 68;
				shulkerEnabledBtn = ButtonWidget.builder(
						Text.literal("Enabled: " + (ModuleConfig.shulkerRefill().enabled() ? "ON" : "OFF")),
						b -> {
							boolean now = !ModuleConfig.shulkerRefill().enabled();
							ModuleConfig.shulkerRefill().enabled(now);
							ModuleConfig.shulkerRefill().save();
							b.setMessage(Text.literal("Enabled: " + (now ? "ON" : "OFF")));
						}
				).dimensions(rightX, optY, 120, 20).build();

				shulkerKeyBtn = ButtonWidget.builder(
						Text.literal("Key: " + KeybindHelper.getKeyName(ModuleConfig.shulkerRefill().shulkerRefillKey())),
						b -> startKeybindCapture(1, b)
				).dimensions(rightX + 125, optY, 120, 20).build();

				addDrawableChild(shulkerEnabledBtn);
				addDrawableChild(shulkerKeyBtn);
			} else if (selectedModule == 2) {
				int optY = contentY + 68;
				boolean remote = this.client != null && this.client.world != null && !this.client.isIntegratedServerRunning();
				String suffix = remote ? " (Server)" : "";
				stackEnabledBtn = ButtonWidget.builder(
						Text.literal("Enabled: " + (ShulkerStackServerConfig.enabled ? "ON" : "OFF") + suffix),
						b -> {
							boolean now = !ShulkerStackServerConfig.enabled;
							ShulkerStackServerConfig.enabled = now;
							ShulkerStackServerConfig.save();
							b.setMessage(Text.literal("Enabled: " + (now ? "ON" : "OFF") + suffix));
						}
				).dimensions(rightX, optY, 120, 20).build();
				stackEnabledBtn.active = !remote;

				addDrawableChild(stackEnabledBtn);
			}
		}
	}

	private boolean keybindMatchesSearch(String sectionTitle, String label, String keybind) {
		if (searchText.isEmpty()) return true;
		return sectionTitle.toLowerCase().contains(searchText)
				|| label.toLowerCase().contains(searchText)
				|| keybind.toLowerCase().contains(searchText)
				|| KeybindHelper.getKeyName(keybind).toLowerCase().contains(searchText);
	}

	private void buildKeybindsTab() {
		int y = CONTENT_Y;
		int labelX = this.width / 2 - 150;
		int btnX = labelX + 110;
		int btnW = 130;

		if (keybindMatchesSearch("Ender Key", "Open Ender Chest:", ModuleConfig.enderChest().openEnderChestKey())) {
			enderKeyKeybindBtn = ButtonWidget.builder(
					Text.literal(KeybindHelper.getKeyName(ModuleConfig.enderChest().openEnderChestKey())),
					b -> startKeybindCapture(0, b)
			).dimensions(btnX, y + 15, btnW, 20).build();
			addDrawableChild(enderKeyKeybindBtn);
		}

		if (keybindMatchesSearch("Shulker Restock", "Mark Slot:", ModuleConfig.shulkerRefill().shulkerRefillKey())) {
			shulkerKeyKeybindBtn = ButtonWidget.builder(
					Text.literal(KeybindHelper.getKeyName(ModuleConfig.shulkerRefill().shulkerRefillKey())),
					b -> startKeybindCapture(1, b)
			).dimensions(btnX, y + 43, btnW, 20).build();
			addDrawableChild(shulkerKeyKeybindBtn);
		}
	}

	private void buildSettingsTab() {
	}

	private void selectModule(int idx) {
		selectedModule = idx;
		waitingForKey = false;
		keybindTarget = -1;
		waitingButton = null;
		fullRebuild();
	}

	private void startKeybindCapture(int target, ButtonWidget button) {
		waitingForKey = true;
		keybindTarget = target;
		capturedMods = 0;
		waitingButton = button;
		button.setMessage(Text.literal("[Press key...]"));
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (waitingForKey) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				cancelKeybind();
				return true;
			}
			if (isModifier(keyCode)) {
				updateMods(keyCode);
				return true;
			}
			String encoded = KeybindHelper.encode(keyCode, capturedMods);
			applyKeybind(encoded);
			return true;
		}

		if (keyCode == GLFW.GLFW_KEY_SLASH && !searchField.isFocused()) {
			searchField.setFocused(true);
			this.setFocused(searchField);
			return true;
		}

		boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
		if (handled) return true;

		if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_F) {
			searchField.setFocused(true);
			this.setFocused(searchField);
			return true;
		}

		return false;
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (waitingForKey) return true;
		return super.charTyped(chr, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (waitingForKey) {
			int mouseCode = -(button + 1);
			String encoded = KeybindHelper.encode(mouseCode, capturedMods);
			applyKeybind(encoded);
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void applyKeybind(String encoded) {
		if (keybindTarget == 0) {
			ModuleConfig.enderChest().openEnderChestKey(encoded);
			ModuleConfig.enderChest().save();
		} else if (keybindTarget == 1) {
			ModuleConfig.shulkerRefill().shulkerRefillKey(encoded);
			ModuleConfig.shulkerRefill().save();
		}
		waitingForKey = false;
		keybindTarget = -1;
		fullRebuild();
	}

	private void cancelKeybind() {
		waitingForKey = false;
		keybindTarget = -1;
		capturedMods = 0;
		fullRebuild();
	}

	private boolean isModifier(int keyCode) {
		return keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL ||
				keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT ||
				keyCode == GLFW.GLFW_KEY_LEFT_ALT || keyCode == GLFW.GLFW_KEY_RIGHT_ALT ||
				keyCode == GLFW.GLFW_KEY_LEFT_SUPER || keyCode == GLFW.GLFW_KEY_RIGHT_SUPER;
	}

	private void updateMods(int keyCode) {
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

		int totalTabsWidth = TAB_WIDTH * 3 + 4;
		int startX = (this.width - totalTabsWidth) / 2;
		drawTabIndicator(context, startX, TAB_Y, activeTab);

		if (activeTab == 0) {
			renderModulesTab(context);
		} else if (activeTab == 1) {
			renderKeybindsTab(context);
		} else if (activeTab == 2) {
			renderSettingsTab(context);
		}
	}

	private void drawTabIndicator(DrawContext context, int startX, int y, int active) {
		int x = startX + active * (TAB_WIDTH + 2);
		context.fill(x, y + 20, x + TAB_WIDTH, y + 22, 0xFFFFFFFF);
	}

	private void renderModulesTab(DrawContext context) {
		if (selectedModule < 0 || !moduleMatchesSearch(selectedModule)) return;

		int contentY = CONTENT_Y;
		int leftX = 10;
		int rightX = leftX + LEFT_WIDTH + 8;
		int rightWidth = this.width - rightX - 10;

		int visibleIdx = 0;
		for (int i = 0; i < selectedModule; i++) {
			if (moduleMatchesSearch(i)) visibleIdx++;
		}
		drawModuleIndicator(context, leftX, contentY, visibleIdx);

		int headerBottom = contentY + 50;
		int sepColor = 0xFF555555;
		context.drawHorizontalLine(rightX, rightX + rightWidth, headerBottom, sepColor);

		String title = getModuleTitle(selectedModule);
		String desc = getModuleDesc(selectedModule);
		context.drawText(this.textRenderer, Text.literal(title), rightX, contentY + 4, 0xFFFFFF, false);
		context.drawText(this.textRenderer, Text.literal(desc), rightX, contentY + 18, 0x808080, false);
		context.drawText(this.textRenderer, Text.literal("Options"), rightX, headerBottom + 8, 0xAAAAAA, false);
	}

	private void drawModuleIndicator(DrawContext context, int leftX, int y, int visibleIdx) {
		context.fill(leftX - 2, y + visibleIdx * 22 - 1, leftX - 1, y + visibleIdx * 22 + 20, 0xFFFFFFFF);
	}

	private void renderKeybindsTab(DrawContext context) {
		int y = CONTENT_Y;
		int labelX = this.width / 2 - 150;
		int sepColor = 0xFF555555;
		int sepEnd = labelX + 100;

		if (keybindMatchesSearch("Ender Key", "Open Ender Chest:", ModuleConfig.enderChest().openEnderChestKey())) {
			context.drawText(this.textRenderer, Text.literal("Ender Key"), labelX, y, 0xFFFFFF, false);
			context.drawHorizontalLine(labelX, sepEnd, y + 10, sepColor);
			context.drawText(this.textRenderer, Text.literal("Open Ender Chest:"), labelX, y + 18, 0x808080, false);
		}

		if (keybindMatchesSearch("Shulker Restock", "Mark Slot:", ModuleConfig.shulkerRefill().shulkerRefillKey())) {
			int y2 = y + 36;
			context.drawText(this.textRenderer, Text.literal("Shulker Restock"), labelX, y2, 0xFFFFFF, false);
			context.drawHorizontalLine(labelX, sepEnd, y2 + 10, sepColor);
			context.drawText(this.textRenderer, Text.literal("Mark Slot:"), labelX, y2 + 18, 0x808080, false);
		}
	}

	private void renderSettingsTab(DrawContext context) {
		drawCenteredText(context, Text.literal("No settings available yet."), this.width / 2, CONTENT_Y + 8, 0x808080);
	}

	private void drawCenteredText(DrawContext context, Text text, int x, int y, int color) {
		int w = this.textRenderer.getWidth(text);
		context.drawText(this.textRenderer, text, x - w / 2, y, color, false);
	}

	private static String getModuleTitle(int idx) {
		return switch (idx) {
			case 0 -> "Ender Key";
			case 1 -> "Shulker Restock";
			case 2 -> "Stackable Shulkers";
			default -> "";
		};
	}

	private static String getModuleDesc(int idx) {
		return switch (idx) {
			case 0 -> "Keybind to open ender chest with any size";
			case 1 -> "Auto-refill from shulker boxes";
			case 2 -> "Shulkers stack up to 64";
			default -> "";
		};
	}

	@Override
	public void close() {
		if (this.client != null) {
			this.client.setScreen(parent);
		}
	}
}
