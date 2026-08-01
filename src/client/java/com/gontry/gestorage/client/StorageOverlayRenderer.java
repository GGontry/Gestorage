package com.gontry.gestorage.client;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.client.config.ModuleConfig;
import com.gontry.gestorage.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class StorageOverlayRenderer {
	private static final Identifier PANEL_TEXTURE = Identifier.of(Gestorage.MOD_ID, "textures/gui/overlay/panel.png");
	private static final int BORDER = 1;
	private static final int PAD_X = 5;
	private static final int PAD_Y = 3;
	private static final int LINE_HEIGHT = 10;
	private static final int ICON_SIZE = 16;
	private static final int ICON_GAP = 3;
	private static final int GAP = 4;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int SUB_COLOR = 0xFFC8C8C8;

	private StorageOverlayRenderer() {}

	public static void render(DrawContext context, HandledScreen<?> screen, int mouseX, int mouseY) {
		if (!ModuleConfig.storageOverlay().enabled()) return;

		HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
		Slot focused = accessor.gestorage_getFocusedSlot();
		if (focused == null) return;

		ItemStack focusedStack = focused.getStack();
		Counts counts = countInventory(screen, focused);
		boolean iconMode = !focusedStack.isEmpty() && ModuleConfig.storageOverlay().showItemIcon();

		List<Text> lines = buildLines(screen, focusedStack, counts, iconMode);
		if (lines.isEmpty()) return;

		TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
		int textWidth = 0;
		for (Text line : lines) {
			textWidth = Math.max(textWidth, renderer.getWidth(line));
		}

		int contentWidth = iconMode ? Math.max(textWidth + ICON_SIZE + ICON_GAP, ICON_SIZE) : textWidth;
		int contentHeight = iconMode ? ICON_SIZE + (lines.size() - 1) * LINE_HEIGHT : lines.size() * LINE_HEIGHT;
		int panelWidth = contentWidth + PAD_X * 2;
		int panelHeight = contentHeight + PAD_Y * 2;

		var config = ModuleConfig.storageOverlay();
		int screenX = accessor.gestorage_getX();
		int screenY = accessor.gestorage_getY();
		int x = screenX - panelWidth - GAP + config.offsetX();
		int y = screenY + config.offsetY();
		if (x < 2) {
			x = screenX + accessor.gestorage_getBackgroundWidth() + GAP + config.offsetX();
		}

		drawPanel(context, x, y, panelWidth, panelHeight);

		int textX = x + PAD_X;
		int textY = y + PAD_Y;
		if (iconMode) {
			context.drawItem(focusedStack, textX, textY);
			context.drawText(renderer, lines.get(0), textX + ICON_SIZE + ICON_GAP, textY + 3, TEXT_COLOR, true);
			textY += ICON_SIZE;
			for (int i = 1; i < lines.size(); i++) {
				context.drawText(renderer, lines.get(i), x + PAD_X, textY, SUB_COLOR, true);
				textY += LINE_HEIGHT;
			}
		} else {
			for (int i = 0; i < lines.size(); i++) {
				int color = i == 0 ? TEXT_COLOR : SUB_COLOR;
				context.drawText(renderer, lines.get(i), textX, textY, color, true);
				textY += LINE_HEIGHT;
			}
		}
	}

	private static Counts countInventory(HandledScreen<?> screen, Slot focused) {
		net.minecraft.inventory.Inventory target = focused.inventory;
		int totalSlots = 0;
		int freeSlots = 0;
		int matchingStacks = 0;
		int matchingItems = 0;
		ItemStack focusedStack = focused.getStack();

		for (Slot slot : screen.getScreenHandler().slots) {
			if (slot.inventory != target) continue;
			totalSlots++;
			ItemStack stack = slot.getStack();
			if (stack.isEmpty()) {
				freeSlots++;
				continue;
			}
			if (!focusedStack.isEmpty() && stack.isOf(focusedStack.getItem())) {
				matchingStacks++;
				matchingItems += stack.getCount();
			}
		}
		return new Counts(totalSlots, freeSlots, matchingStacks, matchingItems);
	}

	private static List<Text> buildLines(HandledScreen<?> screen, ItemStack focusedStack, Counts counts, boolean iconMode) {
		var config = ModuleConfig.storageOverlay();
		List<Text> lines = new ArrayList<>();

		if (iconMode) {
			lines.add(focusedStack.getName());
			if (config.showStackCount()) {
				lines.add(Text.translatable("overlay.gestorage.stacks", counts.matchingStacks()));
			}
			if (config.showItemCount()) {
				lines.add(Text.translatable("overlay.gestorage.items", counts.matchingItems()));
			}
			if (config.showFreeSlots()) {
				lines.add(Text.translatable("overlay.gestorage.free", counts.freeSlots(), counts.totalSlots()));
			}
			return lines;
		}

		if (config.showInventoryName()) {
			lines.add(screen.getTitle());
		}
		if (!focusedStack.isEmpty()) {
			if (config.showItemName()) {
				lines.add(focusedStack.getName());
			}
			if (config.showStackCount()) {
				lines.add(Text.translatable("overlay.gestorage.stacks", counts.matchingStacks()));
			}
			if (config.showItemCount()) {
				lines.add(Text.translatable("overlay.gestorage.items", counts.matchingItems()));
			}
		}
		if (config.showFreeSlots()) {
			lines.add(Text.translatable("overlay.gestorage.free", counts.freeSlots(), counts.totalSlots()));
		}

		return lines;
	}

	private static void drawPanel(DrawContext context, int x, int y, int width, int height) {
		int right = x + width - BORDER;
		int bottom = y + height - BORDER;
		int innerWidth = Math.max(width - BORDER * 2, 0);
		int innerHeight = Math.max(height - BORDER * 2, 0);

		context.drawTexture(PANEL_TEXTURE, x, y, BORDER, BORDER, 0, 0, BORDER, BORDER, 16, 16);
		context.drawTexture(PANEL_TEXTURE, x + BORDER, y, innerWidth, BORDER, BORDER, 0, 16 - BORDER * 2, BORDER, 16, 16);
		context.drawTexture(PANEL_TEXTURE, right, y, BORDER, BORDER, 16 - BORDER, 0, BORDER, BORDER, 16, 16);

		context.drawTexture(PANEL_TEXTURE, x, y + BORDER, BORDER, innerHeight, 0, BORDER, BORDER, 16 - BORDER * 2, 16, 16);
		context.drawTexture(PANEL_TEXTURE, x + BORDER, y + BORDER, innerWidth, innerHeight, BORDER, BORDER, 16 - BORDER * 2, 16 - BORDER * 2, 16, 16);
		context.drawTexture(PANEL_TEXTURE, right, y + BORDER, BORDER, innerHeight, 16 - BORDER, BORDER, BORDER, 16 - BORDER * 2, 16, 16);

		context.drawTexture(PANEL_TEXTURE, x, bottom, BORDER, BORDER, 0, 16 - BORDER, BORDER, BORDER, 16, 16);
		context.drawTexture(PANEL_TEXTURE, x + BORDER, bottom, innerWidth, BORDER, BORDER, 16 - BORDER, 16 - BORDER * 2, BORDER, 16, 16);
		context.drawTexture(PANEL_TEXTURE, right, bottom, BORDER, BORDER, 16 - BORDER, 16 - BORDER, BORDER, BORDER, 16, 16);
	}

	private record Counts(int totalSlots, int freeSlots, int matchingStacks, int matchingItems) {}
}
