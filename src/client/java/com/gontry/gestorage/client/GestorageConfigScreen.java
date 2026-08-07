package com.gontry.gestorage.client;

import com.gontry.gestorage.client.config.ModuleConfig;
import com.gontry.gestorage.client.ui.ConfigButton;
import com.gontry.gestorage.client.ui.ConfigCheckbox;
import com.gontry.gestorage.client.ui.ConfigIconButton;
import com.gontry.gestorage.client.ui.ConfigTextures;
import com.gontry.gestorage.config.ShulkerStackServerConfig;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class GestorageConfigScreen extends Screen {
	private final Screen parent;
	private int selectedModule = 0;
	private boolean waitingForKey = false;
	private int keybindTarget = -1;
	private int capturedMods = 0;
	private String searchText = "";
	private String originalSearchText = "";
	private int scrollOffset = 0;

	private TextFieldWidget searchField;
	private final List<ConfigButton> moduleButtons = new ArrayList<>();
	private final List<DetailRow> detailRows = new ArrayList<>();
	private ConfigIconButton closeButton;
	private ConfigButton keybindButton;

	private int windowX, windowY, windowW, windowH;
	private int searchX, searchY, searchFieldW;
	private int bodyY;
	private int detailX, detailW;
	private int optionsTop;
	private int viewportH;
	private int closeX;

	private static final int PAD = 8;
	private static final int HEADER_H = 16;
	private static final int SEARCH_H = 16;
	private static final int HEADER_GAP = 6;
	private static final int SEARCH_GAP = 6;
	private static final int LEFT_W = 124;
	private static final int COL_GAP = 10;
	private static final int ROW_H = 20;
	private static final int ROW_GAP = 4;
	private static final int DETAIL_HEADER_H = 32;
	private static final int CHROME_H = PAD * 2 + HEADER_H + HEADER_GAP + SEARCH_H + SEARCH_GAP;
	private static final int DESIGNED_WINDOW_H = CHROME_H + 7 * ROW_H + 6 * ROW_GAP + DETAIL_HEADER_H;

	public GestorageConfigScreen(Screen parent) {
		super(Text.literal("Gestorage Settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();
		computeLayout();
		buildChrome();
		buildContent();
		searchField.setFocused(true);
		this.setFocused(searchField);
	}

	private void computeLayout() {
		windowW = 360;
		windowH = Math.min(DESIGNED_WINDOW_H, Math.max(this.height - 12, 80));
		windowX = (this.width - windowW) / 2;
		windowY = (this.height - windowH) / 2;
		searchX = windowX + PAD;
		searchY = windowY + PAD + HEADER_H + HEADER_GAP;
		searchFieldW = windowW - PAD * 2;
		bodyY = searchY + SEARCH_H + SEARCH_GAP;
		detailX = searchX + LEFT_W + COL_GAP;
		detailW = searchFieldW - LEFT_W - COL_GAP;
		optionsTop = bodyY + DETAIL_HEADER_H;
		viewportH = Math.max(0, (windowY + windowH - PAD) - optionsTop);
		closeX = windowX + windowW - PAD - HEADER_H;
	}

	private void buildChrome() {
		this.clearChildren();
		moduleButtons.clear();
		detailRows.clear();
		keybindButton = null;
		scrollOffset = 0;

		closeButton = new ConfigIconButton(closeX, windowY + PAD, HEADER_H, HEADER_H, ConfigTextures.CLOSE, ConfigTextures.CLOSE_HOVER, this::close);
		addDrawableChild(closeButton);

		searchField = new CenteredSearchField(this.textRenderer, searchX, searchY, searchFieldW, SEARCH_H, Text.literal(""));
		searchField.setPlaceholder(Text.literal("Search modules..."));
		searchField.setEditableColor(0xFFFFFFFF);
		searchField.setUneditableColor(0xFF808080);
		searchField.setDrawsBackground(false);
		searchField.setMaxLength(64);
		searchField.setText(originalSearchText);
		searchField.setChangedListener(text -> {
			originalSearchText = text;
			searchText = text.toLowerCase();
			onSearchChanged();
		});
		addDrawableChild(searchField);
	}

	private void buildContent() {
		int y = bodyY;
		for (int i = 0; i < 4; i++) {
			if (!moduleMatchesSearch(i)) continue;
			int idx = i;
			ConfigButton btn = new ConfigButton(searchX, y, LEFT_W, ROW_H,
					Text.literal(getModuleTitle(idx)), () -> selectModule(idx));
			btn.setSelected(idx == selectedModule);
			moduleButtons.add(btn);
			addDrawableChild(btn);
			y += ROW_H + ROW_GAP;
		}

		if (selectedModule >= 0 && selectedModule < 4 && moduleMatchesSearch(selectedModule)) {
			buildDetail(selectedModule);
		}
		positionRows();
	}

	private void rebuildContent() {
		for (ConfigButton btn : moduleButtons) {
			this.remove(btn);
		}
		moduleButtons.clear();
		detailRows.clear();
		keybindButton = null;
		closeButton.setX(closeX);
		closeButton.setY(windowY + PAD);
		searchField.setX(searchX);
		searchField.setY(searchY);
		buildContent();
	}

	private void onSearchChanged() {
		selectedModule = findFirstVisibleModule(0);
		scrollOffset = 0;
		rebuildContent();
		searchField.setFocused(true);
		this.setFocused(searchField);
	}

	private void selectModule(int idx) {
		selectedModule = idx;
		waitingForKey = false;
		keybindTarget = -1;
		scrollOffset = 0;
		rebuildContent();
		searchField.setFocused(true);
		this.setFocused(searchField);
	}

	private void buildDetail(int module) {
		int baseY = 0;
		switch (module) {
			case 0 -> {
				addCheckbox(baseY, Text.literal("Enabled"),
						() -> ModuleConfig.enderChest().enabled(),
						v -> ModuleConfig.enderChest().enabled(v),
						ModuleConfig.enderChest()::save);
				baseY += ROW_H + ROW_GAP;
				keybindButton = new ConfigButton(detailX, optionsTop, detailW, ROW_H,
						Text.literal("Key: " + KeybindHelper.getKeyName(ModuleConfig.enderChest().openEnderChestKey())),
						() -> startKeybindCapture(0));
				detailRows.add(new DetailRow(keybindButton, baseY));
			}
			case 1 -> {
				addCheckbox(baseY, Text.literal("Enabled"),
						() -> ModuleConfig.shulkerRefill().enabled(),
						v -> ModuleConfig.shulkerRefill().enabled(v),
						ModuleConfig.shulkerRefill()::save);
				baseY += ROW_H + ROW_GAP;
				keybindButton = new ConfigButton(detailX, optionsTop, detailW, ROW_H,
						Text.literal("Key: " + KeybindHelper.getKeyName(ModuleConfig.shulkerRefill().shulkerRefillKey())),
						() -> startKeybindCapture(1));
				detailRows.add(new DetailRow(keybindButton, baseY));
			}
			case 2 -> {
				boolean remote = this.client != null && this.client.world != null && !this.client.isIntegratedServerRunning();
				ConfigCheckbox cb = new ConfigCheckbox(detailX, optionsTop, detailW, ROW_H,
						Text.literal(remote ? "Enabled (Server)" : "Enabled"),
						() -> ShulkerStackServerConfig.enabled,
						v -> ShulkerStackServerConfig.enabled = v,
						ShulkerStackServerConfig::save);
				cb.active = !remote;
				detailRows.add(new DetailRow(cb, baseY));
			}
			case 3 -> {
				addCheckbox(baseY, Text.literal("Enabled"),
						() -> ModuleConfig.storageOverlay().enabled(),
						v -> ModuleConfig.storageOverlay().enabled(v),
						ModuleConfig.storageOverlay()::save);
				baseY += ROW_H + ROW_GAP;
				addCheckbox(baseY, Text.literal("Inventory Name"),
						() -> ModuleConfig.storageOverlay().showInventoryName(),
						v -> ModuleConfig.storageOverlay().showInventoryName(v),
						ModuleConfig.storageOverlay()::save);
				baseY += ROW_H + ROW_GAP;
				addCheckbox(baseY, Text.literal("Item Name"),
						() -> ModuleConfig.storageOverlay().showItemName(),
						v -> ModuleConfig.storageOverlay().showItemName(v),
						ModuleConfig.storageOverlay()::save);
				baseY += ROW_H + ROW_GAP;
				addCheckbox(baseY, Text.literal("Item Icon"),
						() -> ModuleConfig.storageOverlay().showItemIcon(),
						v -> ModuleConfig.storageOverlay().showItemIcon(v),
						ModuleConfig.storageOverlay()::save);
				baseY += ROW_H + ROW_GAP;
				addCheckbox(baseY, Text.literal("Stacks"),
						() -> ModuleConfig.storageOverlay().showStackCount(),
						v -> ModuleConfig.storageOverlay().showStackCount(v),
						ModuleConfig.storageOverlay()::save);
				baseY += ROW_H + ROW_GAP;
				addCheckbox(baseY, Text.literal("Items"),
						() -> ModuleConfig.storageOverlay().showItemCount(),
						v -> ModuleConfig.storageOverlay().showItemCount(v),
						ModuleConfig.storageOverlay()::save);
				baseY += ROW_H + ROW_GAP;
				addCheckbox(baseY, Text.literal("Free Slots"),
						() -> ModuleConfig.storageOverlay().showFreeSlots(),
						v -> ModuleConfig.storageOverlay().showFreeSlots(v),
						ModuleConfig.storageOverlay()::save);
			}
		}
	}

	private void addCheckbox(int baseY, Text label, BooleanSupplier getter,
			Consumer<Boolean> setter, Runnable onToggle) {
		ConfigCheckbox cb = new ConfigCheckbox(detailX, optionsTop, detailW, ROW_H, label, getter, setter, onToggle);
		detailRows.add(new DetailRow(cb, baseY));
	}

	private void positionRows() {
		for (DetailRow row : detailRows) {
			row.widget.setX(detailX);
			row.widget.setY(optionsTop + row.baseY - scrollOffset);
		}
	}

	private void updateScroll() {
		int maxScroll = Math.max(0, detailContentHeight() - viewportH);
		scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
		positionRows();
	}

	private int detailContentHeight() {
		if (detailRows.isEmpty()) return 0;
		return detailRows.size() * ROW_H + (detailRows.size() - 1) * ROW_GAP;
	}

	private boolean withinViewport(double mouseX, double mouseY) {
		return mouseX >= detailX && mouseX <= detailX + detailW
				&& mouseY >= optionsTop && mouseY <= optionsTop + viewportH;
	}

	private boolean moduleMatchesSearch(int idx) {
		if (searchText.isEmpty()) return true;
		return getModuleTitle(idx).toLowerCase().contains(searchText)
				|| getModuleDesc(idx).toLowerCase().contains(searchText);
	}

	private int findFirstVisibleModule(int startFrom) {
		for (int i = startFrom; i < 4; i++) {
			if (moduleMatchesSearch(i)) return i;
		}
		return -1;
	}

	private void startKeybindCapture(int target) {
		waitingForKey = true;
		keybindTarget = target;
		capturedMods = 0;
		if (keybindButton != null) {
			keybindButton.setMessage(Text.literal("[Press key...]"));
		}
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
		return super.keyPressed(keyCode, scanCode, modifiers);
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
		if (withinViewport(mouseX, mouseY)) {
			for (DetailRow row : detailRows) {
				if (row.widget.mouseClicked(mouseX, mouseY, button)) {
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (withinViewport(mouseX, mouseY)) {
			scrollOffset -= (int) (verticalAmount * 16);
			updateScroll();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
		selectModule(selectedModule);
	}

	private void cancelKeybind() {
		waitingForKey = false;
		keybindTarget = -1;
		capturedMods = 0;
		selectModule(selectedModule);
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
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);
		if (windowW <= 0) return;
		ConfigTextures.drawNineSlice(context, ConfigTextures.WINDOW, windowX, windowY, windowW, windowH);
		ConfigTextures.drawNineSlice(context, ConfigTextures.BUTTON, searchX, searchY, searchFieldW, SEARCH_H);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		int titleX = windowX + windowW / 2;
		drawCenteredText(context, Text.literal("Gestorage Settings"), titleX, windowY + PAD + 4, 0xFFFFFFFF);

		if (selectedModule >= 0 && selectedModule < 4 && moduleMatchesSearch(selectedModule)) {
			context.drawText(this.textRenderer, Text.literal(getModuleTitle(selectedModule)), detailX, bodyY, 0xFFFFFFFF, false);
			List<OrderedText> descLines = this.textRenderer.wrapLines(Text.literal(getModuleDesc(selectedModule)), detailW);
			int descY = bodyY + 10;
			for (OrderedText line : descLines) {
				context.drawText(this.textRenderer, line, detailX, descY, 0xFF9A9A9A, false);
				descY += 9;
			}
			context.drawHorizontalLine(detailX, detailX + detailW, bodyY + DETAIL_HEADER_H - 3, 0xFF4A4A4A);

			context.enableScissor(detailX, optionsTop, detailX + detailW, optionsTop + viewportH);
			for (DetailRow row : detailRows) {
				row.widget.render(context, mouseX, mouseY, delta);
			}
			context.disableScissor();
			renderScrollbar(context);
		} else {
			drawCenteredText(context, Text.literal("No matching modules"), windowX + windowW / 2, bodyY + 10, 0xFF9A9A9A);
		}
	}

	private void renderScrollbar(DrawContext context) {
		int maxScroll = detailContentHeight() - viewportH;
		if (maxScroll <= 0) return;
		int trackH = viewportH - 8;
		int thumbH = Math.max(8, viewportH * viewportH / detailContentHeight());
		int thumbY = optionsTop + (maxScroll == 0 ? 0 : (int) ((long) scrollOffset * trackH / maxScroll));
		context.fill(detailX + detailW - 2, thumbY, detailX + detailW, thumbY + thumbH, 0xFF9A9A9A);
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
			case 3 -> "Storage Overlay";
			default -> "";
		};
	}

	private static String getModuleDesc(int idx) {
		return switch (idx) {
			case 0 -> "Keybind to open ender chest with any size";
			case 1 -> "Auto-refill from shulker boxes";
			case 2 -> "Shulkers stack up to 64";
			case 3 -> "Informational overlay next to any inventory";
			default -> "";
		};
	}

	private record DetailRow(PressableWidget widget, int baseY) {}

	private static class CenteredSearchField extends TextFieldWidget {
		CenteredSearchField(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
			super(textRenderer, x, y, width, height, text);
		}

		@Override
		public int getInnerWidth() {
			return this.width - 8;
		}

		@Override
		public void onClick(double mouseX, double mouseY) {
			super.onClick(mouseX - 4, mouseY);
		}

		@Override
		public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
			context.getMatrices().push();
			context.getMatrices().translate(4, (this.height - 8) / 2f, 0);
			super.renderWidget(context, mouseX, mouseY, delta);
			context.getMatrices().pop();
		}
	}

	@Override
	public void close() {
		if (this.client != null) {
			this.client.setScreen(parent);
		}
	}
}
