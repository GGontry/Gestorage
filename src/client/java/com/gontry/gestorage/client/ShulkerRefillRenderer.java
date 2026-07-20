package com.gontry.gestorage.client;

import com.gontry.gestorage.refill.ShulkerLink;
import com.gontry.gestorage.refill.ShulkerLinkManager;
import com.gontry.gestorage.screen.ExtraLargeEnderScreen;
import com.gontry.gestorage.screen.LargeEnderScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;

import java.util.List;

public class ShulkerRefillRenderer {
	private static final int LINKED_BORDER_COLOR = 0xCC9932CC;
	private static final int MARKED_BORDER_COLOR = 0xCC00FF00;

	public static void renderSlotBorder(DrawContext context, Slot slot, HandledScreen<?> screen) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) return;

		String worldKey = WorldKeyHelper.getFullWorldKey(client);
		if (worldKey == null) return;
		String slotType = getSlotType(slot, screen);
		if (slotType == null) return;

		int marked = ShulkerRefillKeybinds.getMarkedSlot();
		String markedType = ShulkerRefillKeybinds.getMarkedSlotType();
		if (marked >= 0 && slot.getIndex() == marked && markedType.equals(slotType)) {
			drawOutline(context, slot.x, slot.y, 16, 16, MARKED_BORDER_COLOR);
			return;
		}

		List<ShulkerLink> links = ShulkerLinkManager.getLinksForWorld(worldKey);
		for (ShulkerLink link : links) {
			if ((slot.getIndex() == link.sourceSlot() && slotType.equals(link.sourceType())) ||
				(slot.getIndex() == link.targetSlot() && slotType.equals(link.targetType()))) {
				drawOutline(context, slot.x, slot.y, 16, 16, LINKED_BORDER_COLOR);
				return;
			}
		}
	}

	static String getSlotType(Slot slot, HandledScreen<?> screen) {
		if (slot.inventory instanceof net.minecraft.entity.player.PlayerInventory) return "player";

		if (screen instanceof LargeEnderScreen) {
			if (!(slot.inventory instanceof net.minecraft.entity.player.PlayerInventory)) {
				return "ender_large";
			}
		}
		if (screen instanceof ExtraLargeEnderScreen) {
			if (!(slot.inventory instanceof net.minecraft.entity.player.PlayerInventory)) {
				return "ender_xlarge";
			}
		}

		return null;
	}

	private static void drawOutline(DrawContext context, int x, int y, int width, int height, int color) {
		context.fill(x, y, x + width, y + 1, color);
		context.fill(x, y + height - 1, x + width, y + height, color);
		context.fill(x, y + 1, x + 1, y + height - 1, color);
		context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
	}
}
