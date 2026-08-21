package com.gontry.gestorage.client;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.client.config.ModuleConfig;
import com.gontry.gestorage.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class InventorySortingRenderer {
	private static final int BUTTON_SIZE = 12;

	private static final Identifier SORT_ICON = Identifier.of(Gestorage.MOD_ID, "textures/gui/config/sort_icon.png");
	private static final Identifier SORT_ICON_HOVER = Identifier.of(Gestorage.MOD_ID, "textures/gui/config/sort_icon_hover.png");
	private static final Identifier SORT_ICON_PRESSED = Identifier.of(Gestorage.MOD_ID, "textures/gui/config/sort_icon_pressed.png");

	private static int buttonX, buttonY;

	private InventorySortingRenderer() {}

	private static void computeButtonPos(HandledScreen<?> screen) {
		HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
		buttonX = accessor.gestorage_getX() + accessor.gestorage_getBackgroundWidth() - BUTTON_SIZE - 4;
		buttonY = accessor.gestorage_getY() + 2;
	}

	public static void renderButton(DrawContext context, HandledScreen<?> screen, int mouseX, int mouseY) {
		if (!ModuleConfig.inventorySorting().enabled()) return;
		if (!ModuleConfig.inventorySorting().showButtons()) return;
		if (!isSortableScreen(screen)) return;

		computeButtonPos(screen);

		if (isInventoryBlocked(screen)) return;

		boolean hovered = mouseX >= buttonX && mouseX < buttonX + BUTTON_SIZE
				&& mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE;

		Identifier texture;
		if (hovered && isPressed()) {
			texture = SORT_ICON_PRESSED;
		} else if (hovered) {
			texture = SORT_ICON_HOVER;
		} else {
			texture = SORT_ICON;
		}

		context.drawTexture(texture, buttonX, buttonY, 0, 0, BUTTON_SIZE, BUTTON_SIZE, 12, 12);
	}

	private static boolean isPressed() {
		long handle = MinecraftClient.getInstance().getWindow().getHandle();
		return GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
	}

	public static boolean handleClick(HandledScreen<?> screen, int mouseX, int mouseY) {
		if (!ModuleConfig.inventorySorting().enabled()) return false;
		if (!ModuleConfig.inventorySorting().showButtons()) return false;
		if (!isSortableScreen(screen)) return false;

		computeButtonPos(screen);

		if (mouseX < buttonX || mouseX >= buttonX + BUTTON_SIZE) return false;
		if (mouseY < buttonY || mouseY >= buttonY + BUTTON_SIZE) return false;
		if (isInventoryBlocked(screen)) return false;

		ModNetworkingClient.sendSortInventory(
				ModuleConfig.inventorySorting().mergeStacks(),
				ModuleConfig.inventorySorting().sortByName(),
				ModuleConfig.inventorySorting().sortDescending()
		);
		return true;
	}

	public static boolean isInventoryBlocked(HandledScreen<?> screen) {
		String type = getInventoryType(screen);
		return switch (type) {
			case "player" -> ModuleConfig.inventorySorting().blockPlayer();
			case "ender_normal", "ender_large", "ender_xlarge" -> ModuleConfig.inventorySorting().blockEnderChest();
			case "shulker_box" -> ModuleConfig.inventorySorting().blockShulkerBox();
			case "generic_container" -> ModuleConfig.inventorySorting().blockGenericContainer();
			default -> false;
		};
	}

	public static String getInventoryType(HandledScreen<?> screen) {
		if (screen instanceof InventoryScreen) return "player";
		if (screen instanceof ShulkerBoxScreen) return "shulker_box";
		if (screen instanceof GenericContainerScreen) {
			return detectGenericContainerType(screen);
		}
		if (screen instanceof com.gontry.gestorage.screen.InventoryTypeProvider provider) {
			return provider.getInventoryType();
		}
		return "unknown";
	}

	private static String detectGenericContainerType(HandledScreen<?> screen) {
		for (Slot slot : screen.getScreenHandler().slots) {
			if (slot.inventory instanceof EnderChestInventory) {
				return "ender_normal";
			}
		}
		return "generic_container";
	}

	private static boolean isSortableScreen(HandledScreen<?> screen) {
		return screen instanceof GenericContainerScreen
				|| screen instanceof ShulkerBoxScreen
				|| screen instanceof InventoryScreen
				|| screen instanceof com.gontry.gestorage.screen.InventoryTypeProvider;
	}
}
