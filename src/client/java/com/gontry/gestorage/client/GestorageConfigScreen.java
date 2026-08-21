package com.gontry.gestorage.client;

import com.gontry.gestorage.client.config.ModuleConfig;
import com.gontry.gestorage.client.ui.ConfigButton;
import com.gontry.gestorage.client.ui.ConfigCheckbox;
import com.gontry.gestorage.client.ui.ConfigIconButton;
import com.gontry.gestorage.client.ui.ConfigTextures;
import com.gontry.gestorage.client.CarefulBreakKeybinds;
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
	private boolean swallowNextChar = false;
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
	private static final int KEY_W = 84;
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
		for (int i = 0; i < 6; i++) {
			if (!moduleMatchesSearch(i)) continue;
			int idx = i;
			ConfigButton btn = new ConfigButton(searchX, y, LEFT_W, ROW_H,
					Text.literal(getModuleTitle(idx)), () -> selectModule(idx));
			btn.setSelected(idx == selectedModule);
			moduleButtons.add(btn);
			addDrawableChild(btn);
			y += ROW_H + ROW_GAP;
		}

		if (selectedModule >= 0 && selectedModule < 6 && moduleMatchesSearch(selectedModule)) {
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
				addOptionWithKey(baseY, "Enabled",
						() -> ModuleConfig.enderChest().enabled(),
						v -> ModuleConfig.enderChest().enabled(v),
						ModuleConfig.enderChest()::save,
						ModuleConfig.enderChest().toggleEnabledKey(), 20);
				baseY += ROW_H + ROW_GAP;
				keybindButton = new ConfigButton(detailX, optionsTop, detailW, ROW_H,
						Text.literal("Open Key: " + KeybindHelper.getKeyName(ModuleConfig.enderChest().openEnderChestKey())),
						() -> startKeybindCapture(0));
				detailRows.add(new DetailRow(keybindButton, 0, detailW, baseY));
			}
			case 1 -> {
				addOptionWithKey(baseY, "Enabled",
						() -> ModuleConfig.shulkerRefill().enabled(),
						v -> ModuleConfig.shulkerRefill().enabled(v),
						ModuleConfig.shulkerRefill()::save,
						ModuleConfig.shulkerRefill().toggleEnabledKey(), 21);
				baseY += ROW_H + ROW_GAP;
				keybindButton = new ConfigButton(detailX, optionsTop, detailW, ROW_H,
						Text.literal("Mark Key: " + KeybindHelper.getKeyName(ModuleConfig.shulkerRefill().shulkerRefillKey())),
						() -> startKeybindCapture(1));
				detailRows.add(new DetailRow(keybindButton, 0, detailW, baseY));
			}
			case 2 -> {
				boolean remote = this.client != null && this.client.world != null && !this.client.isIntegratedServerRunning();
				int cbW = detailW - KEY_W - 4;
				ConfigCheckbox cb = new ConfigCheckbox(detailX, optionsTop, cbW, ROW_H,
						Text.literal(remote ? "Enabled (Server)" : "Enabled"),
						() -> ShulkerStackServerConfig.enabled,
						v -> ShulkerStackServerConfig.enabled = v,
						() -> {
							ShulkerStackServerConfig.save();
							if (this.client != null && this.client.player != null) {
								this.client.player.sendMessage(Text.literal(
										"§7Stackable Shulkers: " + (ShulkerStackServerConfig.enabled ? "§aON" : "§cOFF")), true);
							}
						});
				cb.active = !remote;
				detailRows.add(new DetailRow(cb, 0, cbW, baseY));
				attachKeyButton(baseY, ShulkerStackKeybinds.toggleEnabledKey, 22);
			}
			case 3 -> {
				addOptionWithKey(baseY, "Enabled",
						() -> ModuleConfig.storageOverlay().enabled(),
						v -> ModuleConfig.storageOverlay().enabled(v),
						ModuleConfig.storageOverlay()::save,
						ModuleConfig.storageOverlay().toggleEnabledKey(), 23);
				baseY += ROW_H + ROW_GAP;
				addOptionWithKey(baseY, "Inventory Name",
						() -> ModuleConfig.storageOverlay().showInventoryName(),
						v -> ModuleConfig.storageOverlay().showInventoryName(v),
						ModuleConfig.storageOverlay()::save,
						ModuleConfig.storageOverlay().toggleInventoryNameKey(), 24);
				baseY += ROW_H + ROW_GAP;
				addOptionWithKey(baseY, "Item Name",
						() -> ModuleConfig.storageOverlay().showItemName(),
						v -> ModuleConfig.storageOverlay().showItemName(v),
						ModuleConfig.storageOverlay()::save,
						ModuleConfig.storageOverlay().toggleItemNameKey(), 25);
				baseY += ROW_H + ROW_GAP;
				addOptionWithKey(baseY, "Item Icon",
						() -> ModuleConfig.storageOverlay().showItemIcon(),
						v -> ModuleConfig.storageOverlay().showItemIcon(v),
						ModuleConfig.storageOverlay()::save,
						ModuleConfig.storageOverlay().toggleItemIconKey(), 26);
				baseY += ROW_H + ROW_GAP;
				addOptionWithKey(baseY, "Stacks",
						() -> ModuleConfig.storageOverlay().showStackCount(),
						v -> ModuleConfig.storageOverlay().showStackCount(v),
						ModuleConfig.storageOverlay()::save,
						ModuleConfig.storageOverlay().toggleStackCountKey(), 27);
				baseY += ROW_H + ROW_GAP;
				addOptionWithKey(baseY, "Items",
						() -> ModuleConfig.storageOverlay().showItemCount(),
						v -> ModuleConfig.storageOverlay().showItemCount(v),
						ModuleConfig.storageOverlay()::save,
						ModuleConfig.storageOverlay().toggleItemCountKey(), 28);
				baseY += ROW_H + ROW_GAP;
				addOptionWithKey(baseY, "Free Slots",
						() -> ModuleConfig.storageOverlay().showFreeSlots(),
						v -> ModuleConfig.storageOverlay().showFreeSlots(v),
						ModuleConfig.storageOverlay()::save,
						ModuleConfig.storageOverlay().toggleFreeSlotsKey(), 29);
			}
			case 4 -> {
				addOptionWithKey(baseY, "Enabled",
						() -> ModuleConfig.inventorySorting().enabled(),
						v -> ModuleConfig.inventorySorting().enabled(v),
						ModuleConfig.inventorySorting()::save,
						ModuleConfig.inventorySorting().toggleEnabledKey(), 30);
				baseY += ROW_H + ROW_GAP;
				addOptionWithKey(baseY, "Show Buttons",
						() -> ModuleConfig.inventorySorting().showButtons(),
						v -> ModuleConfig.inventorySorting().showButtons(v),
						ModuleConfig.inventorySorting()::save,
						ModuleConfig.inventorySorting().toggleShowButtonsKey(), 31);
				baseY += ROW_H + ROW_GAP;
				keybindButton = new ConfigButton(detailX, optionsTop, detailW, ROW_H,
						Text.literal("Sort Key: " + KeybindHelper.getKeyName(ModuleConfig.inventorySorting().sortKey())),
						() -> startKeybindCapture(2));
				detailRows.add(new DetailRow(keybindButton, 0, detailW, baseY));
				baseY += ROW_H + ROW_GAP;
				addOptionWithKey(baseY, "Merge Stacks",
						() -> ModuleConfig.inventorySorting().mergeStacks(),
						v -> ModuleConfig.inventorySorting().mergeStacks(v),
						ModuleConfig.inventorySorting()::save,
						ModuleConfig.inventorySorting().toggleMergeStacksKey(), 32);
				baseY += ROW_H + ROW_GAP;
				addOptionWithKey(baseY, "Sort By Name",
						() -> ModuleConfig.inventorySorting().sortByName(),
						v -> ModuleConfig.inventorySorting().sortByName(v),
						ModuleConfig.inventorySorting()::save,
						ModuleConfig.inventorySorting().toggleSortByNameKey(), 33);
				baseY += ROW_H + ROW_GAP;
				addOptionWithKey(baseY, "Sort Descending",
						() -> ModuleConfig.inventorySorting().sortDescending(),
						v -> ModuleConfig.inventorySorting().sortDescending(v),
						ModuleConfig.inventorySorting()::save,
						ModuleConfig.inventorySorting().toggleSortDescendingKey(), 34);
				baseY += ROW_H + ROW_GAP;
				addOptionWithKey(baseY, "Block Player Inventory",
						() -> ModuleConfig.inventorySorting().blockPlayer(),
						v -> ModuleConfig.inventorySorting().blockPlayer(v),
						ModuleConfig.inventorySorting()::save,
						ModuleConfig.inventorySorting().toggleBlockPlayerKey(), 35);
				baseY += ROW_H + ROW_GAP;
				addOptionWithKey(baseY, "Block Ender Chest",
						() -> ModuleConfig.inventorySorting().blockEnderChest(),
						v -> ModuleConfig.inventorySorting().blockEnderChest(v),
						ModuleConfig.inventorySorting()::save,
						ModuleConfig.inventorySorting().toggleBlockEnderChestKey(), 36);
				baseY += ROW_H + ROW_GAP;
				addOptionWithKey(baseY, "Block Shulker Box",
						() -> ModuleConfig.inventorySorting().blockShulkerBox(),
						v -> ModuleConfig.inventorySorting().blockShulkerBox(v),
						ModuleConfig.inventorySorting()::save,
						ModuleConfig.inventorySorting().toggleBlockShulkerBoxKey(), 37);
				baseY += ROW_H + ROW_GAP;
				addOptionWithKey(baseY, "Block Chest/Barrel",
						() -> ModuleConfig.inventorySorting().blockGenericContainer(),
						v -> ModuleConfig.inventorySorting().blockGenericContainer(v),
						ModuleConfig.inventorySorting()::save,
						ModuleConfig.inventorySorting().toggleBlockGenericContainerKey(), 38);
			}
			case 5 -> {
				addServerOptionWithKey(baseY, "Enabled", 6, CarefulBreakKeybinds.enabledKey, 16);
				baseY += ROW_H + ROW_GAP;
				addServerOptionWithKey(baseY, "Careful Break", 0, CarefulBreakKeybinds.carefulBreakKey, 10);
				baseY += ROW_H + ROW_GAP;
				addServerOptionWithKey(baseY, "Careful Drop", 1, CarefulBreakKeybinds.carefulDropKey, 11);
				baseY += ROW_H + ROW_GAP;
				addServerOptionWithKey(baseY, "Always Careful", 2, CarefulBreakKeybinds.alwaysCarefulKey, 12);
				baseY += ROW_H + ROW_GAP;
				addServerOptionWithKey(baseY, "Tree Capitator", 3, CarefulBreakKeybinds.treeCapitatorKey, 13);
				baseY += ROW_H + ROW_GAP;
				addServerOptionWithKey(baseY, "Better Harvesting", 4, CarefulBreakKeybinds.betterHarvestingKey, 14);
				baseY += ROW_H + ROW_GAP;
				addServerOptionWithKey(baseY, "Auto Replant", 5, CarefulBreakKeybinds.autoReplantKey, 15);
			}
		}
	}

	private void addCheckbox(int baseY, Text label, BooleanSupplier getter,
			Consumer<Boolean> setter, Runnable onToggle) {
		ConfigCheckbox cb = new ConfigCheckbox(detailX, optionsTop, detailW, ROW_H, label, getter, setter, onToggle);
		detailRows.add(new DetailRow(cb, 0, detailW, baseY));
	}

	private void addOptionWithKey(int baseY, String label, BooleanSupplier getter,
			Consumer<Boolean> setter, Runnable onSave, String currentKey, int targetId) {
		int cbW = detailW - KEY_W - 4;
		ConfigCheckbox cb = new ConfigCheckbox(detailX, optionsTop, cbW, ROW_H,
				Text.literal(label), getter, setter, onSave);
		detailRows.add(new DetailRow(cb, 0, cbW, baseY));
		attachKeyButton(baseY, currentKey, targetId);
	}

	private void attachKeyButton(int baseY, String currentKey, int targetId) {
		ConfigButton[] ref = new ConfigButton[1];
		ConfigButton btn = new ConfigButton(detailX + detailW - KEY_W, optionsTop, KEY_W, ROW_H,
				Text.literal(KeybindHelper.getKeyName(currentKey).replace(" ", "")),
				() -> {
					keybindButton = ref[0];
					startKeybindCapture(targetId);
				});
		ref[0] = btn;
		detailRows.add(new DetailRow(btn, detailW - KEY_W, KEY_W, baseY));
	}

	private void addServerCheckbox(int baseY, String label, int optionId) {
		ConfigCheckbox cb = new ConfigCheckbox(detailX, optionsTop, detailW, ROW_H,
				Text.literal(label),
				() -> serverOptionState(optionId),
				v -> {},
				() -> ModNetworkingClient.sendToggleCarefulBreak(optionId));
		detailRows.add(new DetailRow(cb, 0, detailW, baseY));
	}

	private static boolean serverOptionState(int optionId) {
		return switch (optionId) {
			case 0 -> ClientCarefulBreakState.carefulBreak;
			case 1 -> ClientCarefulBreakState.carefulDrop;
			case 2 -> ClientCarefulBreakState.alwaysCareful;
			case 3 -> ClientCarefulBreakState.treeCapitator;
			case 4 -> ClientCarefulBreakState.betterHarvesting;
			case 5 -> ClientCarefulBreakState.autoReplant;
			default -> ClientCarefulBreakState.enabled;
		};
	}

	private void addServerOptionWithKey(int baseY, String label, int optionId, String currentKey, int targetId) {
		int cbW = detailW - KEY_W - 4;
		ConfigCheckbox cb = new ConfigCheckbox(detailX, optionsTop, cbW, ROW_H,
				Text.literal(label),
				() -> serverOptionState(optionId),
				v -> {},
				() -> ModNetworkingClient.sendToggleCarefulBreak(optionId));
		detailRows.add(new DetailRow(cb, 0, cbW, baseY));
		attachKeyButton(baseY, currentKey, targetId);
	}

	private void positionRows() {
		for (DetailRow row : detailRows) {
			row.widget.setX(detailX + row.offsetX);
			row.widget.setWidth(row.width);
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
		int maxBase = 0;
		for (DetailRow row : detailRows) {
			maxBase = Math.max(maxBase, row.baseY);
		}
		return maxBase + ROW_H;
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
		for (int i = startFrom; i < 6; i++) {
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
		swallowNextChar = false;
		if (waitingForKey) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				clearKeybind();
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
		if (swallowNextChar) {
			swallowNextChar = false;
			return true;
		}
		if (waitingForKey) return true;
		return super.charTyped(chr, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (waitingForKey) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
				cancelKeybindCapture();
				return true;
			}
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

	private void cancelKeybindCapture() {
		waitingForKey = false;
		keybindTarget = -1;
		capturedMods = 0;
		if (this.client != null && this.client.player != null) {
			this.client.player.sendMessage(Text.literal("§7Keybind capture cancelled."), false);
		}
		selectModule(selectedModule);
	}

	private void applyKeybind(String encoded) {
		writeKeybind(keybindTarget, encoded);
		waitingForKey = false;
		keybindTarget = -1;
		swallowNextChar = true;
		selectModule(selectedModule);
	}

	private void clearKeybind() {
		writeKeybind(keybindTarget, "");
		waitingForKey = false;
		keybindTarget = -1;
		capturedMods = 0;
		selectModule(selectedModule);
	}

	private void writeKeybind(int target, String encoded) {
		switch (target) {
			case 0 -> { ModuleConfig.enderChest().openEnderChestKey(encoded); ModuleConfig.enderChest().save(); }
			case 1 -> { ModuleConfig.shulkerRefill().shulkerRefillKey(encoded); ModuleConfig.shulkerRefill().save(); }
			case 2 -> { ModuleConfig.inventorySorting().sortKey(encoded); ModuleConfig.inventorySorting().save(); }
			case 16 -> { CarefulBreakKeybinds.enabledKey = encoded; CarefulBreakKeybinds.save(); }
			case 20 -> { ModuleConfig.enderChest().toggleEnabledKey(encoded); ModuleConfig.enderChest().save(); }
			case 21 -> { ModuleConfig.shulkerRefill().toggleEnabledKey(encoded); ModuleConfig.shulkerRefill().save(); }
			case 22 -> { ShulkerStackKeybinds.toggleEnabledKey = encoded; ShulkerStackKeybinds.save(); }
			case 23 -> { ModuleConfig.storageOverlay().toggleEnabledKey(encoded); ModuleConfig.storageOverlay().save(); }
			case 24 -> { ModuleConfig.storageOverlay().toggleInventoryNameKey(encoded); ModuleConfig.storageOverlay().save(); }
			case 25 -> { ModuleConfig.storageOverlay().toggleItemNameKey(encoded); ModuleConfig.storageOverlay().save(); }
			case 26 -> { ModuleConfig.storageOverlay().toggleItemIconKey(encoded); ModuleConfig.storageOverlay().save(); }
			case 27 -> { ModuleConfig.storageOverlay().toggleStackCountKey(encoded); ModuleConfig.storageOverlay().save(); }
			case 28 -> { ModuleConfig.storageOverlay().toggleItemCountKey(encoded); ModuleConfig.storageOverlay().save(); }
			case 29 -> { ModuleConfig.storageOverlay().toggleFreeSlotsKey(encoded); ModuleConfig.storageOverlay().save(); }
			case 30 -> { ModuleConfig.inventorySorting().toggleEnabledKey(encoded); ModuleConfig.inventorySorting().save(); }
			case 31 -> { ModuleConfig.inventorySorting().toggleShowButtonsKey(encoded); ModuleConfig.inventorySorting().save(); }
			case 32 -> { ModuleConfig.inventorySorting().toggleMergeStacksKey(encoded); ModuleConfig.inventorySorting().save(); }
			case 33 -> { ModuleConfig.inventorySorting().toggleSortByNameKey(encoded); ModuleConfig.inventorySorting().save(); }
			case 34 -> { ModuleConfig.inventorySorting().toggleSortDescendingKey(encoded); ModuleConfig.inventorySorting().save(); }
			case 35 -> { ModuleConfig.inventorySorting().toggleBlockPlayerKey(encoded); ModuleConfig.inventorySorting().save(); }
			case 36 -> { ModuleConfig.inventorySorting().toggleBlockEnderChestKey(encoded); ModuleConfig.inventorySorting().save(); }
			case 37 -> { ModuleConfig.inventorySorting().toggleBlockShulkerBoxKey(encoded); ModuleConfig.inventorySorting().save(); }
			case 38 -> { ModuleConfig.inventorySorting().toggleBlockGenericContainerKey(encoded); ModuleConfig.inventorySorting().save(); }
			default -> applyCBKeybind(encoded);
		}
	}

	private void applyCBKeybind(String encoded) {
		switch (keybindTarget) {
			case 10 -> CarefulBreakKeybinds.carefulBreakKey = encoded;
			case 11 -> CarefulBreakKeybinds.carefulDropKey = encoded;
			case 12 -> CarefulBreakKeybinds.alwaysCarefulKey = encoded;
			case 13 -> CarefulBreakKeybinds.treeCapitatorKey = encoded;
			case 14 -> CarefulBreakKeybinds.betterHarvestingKey = encoded;
			case 15 -> CarefulBreakKeybinds.autoReplantKey = encoded;
		}
		CarefulBreakKeybinds.save();
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

		if (selectedModule >= 0 && selectedModule < 6 && moduleMatchesSearch(selectedModule)) {
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
		int scrollRange = Math.max(1, trackH - thumbH);
		int thumbY = optionsTop + (int) ((long) scrollOffset * scrollRange / maxScroll);
		thumbY = Math.min(thumbY, optionsTop + scrollRange);
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
			case 4 -> "Inventory Sorting";
			case 5 -> "Careful Break";
			default -> "";
		};
	}

	private static String getModuleDesc(int idx) {
		return switch (idx) {
			case 0 -> "Keybind to open ender chest with any size";
			case 1 -> "Auto-refill from shulker boxes";
			case 2 -> "Shulkers stack up to 64";
			case 3 -> "Informational overlay next to any inventory";
			case 4 -> "Sort items in inventories";
			case 5 -> "Collect blocks and drops directly to inventory";
			default -> "";
		};
	}

	private record DetailRow(PressableWidget widget, int offsetX, int width, int baseY) {}

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
