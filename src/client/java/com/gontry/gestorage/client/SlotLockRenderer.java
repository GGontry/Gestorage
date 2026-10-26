package com.gontry.gestorage.client;

import com.gontry.gestorage.ModConstants;
import com.gontry.gestorage.client.config.ModuleConfig;
import com.gontry.gestorage.mixin.HandledScreenAccessor;
import com.gontry.gestorage.screen.InventoryTypeProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

/**
 * Draws and toggles slot locks.
 *
 * <p>Holding the configured key outlines every lockable slot in yellow; clicking
 * one sends a toggle to the server, which owns the real state. Locked slots always
 * show a red outline, whether or not the key is held.
 */
public final class SlotLockRenderer {
	private static final int SLOT_SIZE = 16;
	private static final int AVAILABLE_BORDER_COLOR = 0xCCFFD700;
	private static final int LOCKED_BORDER_COLOR = 0xCCFF3030;

	private static boolean modifierHeld;

	private SlotLockRenderer() {}

	/** Samples the keybind once per screen frame so drawSlot does not poll GLFW per slot. */
	public static void beginFrame() {
		modifierHeld = false;
		if (!ModuleConfig.slotLock().enabled()) return;

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.getWindow() == null) return;

		String key = ModuleConfig.slotLock().lockKey();
		if (key.isEmpty()) return;

		modifierHeld = KeybindHelper.isPressed(key, client.getWindow().getHandle());
	}

	public static void renderSlotBorder(DrawContext context, Slot slot, HandledScreen<?> screen) {
		if (!ModuleConfig.slotLock().enabled()) return;

		int scope = scopeOf(slot, screen);
		if (scope < 0) return;

		boolean locked = scope == ModConstants.SLOT_SCOPE_PLAYER
				? ClientSlotLockState.isPlayerLocked(slot.getIndex())
				: ClientSlotLockState.isEnderLocked(slot.getIndex());

		if (locked) {
			drawOutline(context, slot.x, slot.y, LOCKED_BORDER_COLOR);
		} else if (modifierHeld) {
			drawOutline(context, slot.x, slot.y, AVAILABLE_BORDER_COLOR);
		}
	}

	public static boolean handleClick(HandledScreen<?> screen, double mouseX, double mouseY) {
		if (!ModuleConfig.slotLock().enabled()) return false;

		String key = ModuleConfig.slotLock().lockKey();
		if (key.isEmpty()) return false;

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.getWindow() == null) return false;
		if (!KeybindHelper.isPressed(key, client.getWindow().getHandle())) return false;

		Slot slot = ((HandledScreenAccessor) screen).gestorage_getSlotAt(mouseX, mouseY);
		if (slot == null) return false;

		int scope = scopeOf(slot, screen);
		if (scope < 0) return false;

		ModNetworkingClient.sendSlotLockToggle(scope, slot.getIndex());
		return true;
	}

	private static int scopeOf(Slot slot, HandledScreen<?> screen) {
		if (slot.inventory instanceof PlayerInventory) {
			return ModConstants.SLOT_SCOPE_PLAYER;
		}
		if (slot.inventory instanceof EnderChestInventory) {
			return ModConstants.SLOT_SCOPE_ENDER;
		}
		if (isEnderScreen(screen)) {
			return ModConstants.SLOT_SCOPE_ENDER;
		}
		return -1;
	}

	/**
	 * The client handler for a vanilla ender chest is a {@code GenericContainerScreen}
	 * backed by a {@code SimpleInventory}, so inventory type is not enough: identify it
	 * by its translation key. Large/xlarge ender chests expose their type directly.
	 */
	private static boolean isEnderScreen(HandledScreen<?> screen) {
		if (screen instanceof InventoryTypeProvider provider) {
			String type = provider.getInventoryType();
			return "ender_large".equals(type) || "ender_xlarge".equals(type);
		}
		if (screen instanceof GenericContainerScreen) {
			Text title = screen.getTitle();
			return title.getContent() instanceof TranslatableTextContent content
					&& "container.enderchest".equals(content.getKey());
		}
		return false;
	}

	private static void drawOutline(DrawContext context, int x, int y, int color) {
		context.fill(x, y, x + SLOT_SIZE, y + 1, color);
		context.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, color);
		context.fill(x, y + 1, x + 1, y + SLOT_SIZE - 1, color);
		context.fill(x + SLOT_SIZE - 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE - 1, color);
	}
}
