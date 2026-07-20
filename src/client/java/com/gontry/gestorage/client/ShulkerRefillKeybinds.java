package com.gontry.gestorage.client;

import com.gontry.gestorage.mixin.HandledScreenAccessor;
import com.gontry.gestorage.refill.ShulkerLink;
import com.gontry.gestorage.refill.ShulkerLinkManager;
import com.gontry.gestorage.refill.ShulkerRefillManager;
import com.gontry.gestorage.screen.ExtraLargeEnderScreen;
import com.gontry.gestorage.screen.LargeEnderScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;

public class ShulkerRefillKeybinds {
	private static int markedSlot = -1;
	private static String markedSlotType = "";
	private static boolean wasPressed = false;

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;
			if (client.getWindow() == null) return;

			long handle = client.getWindow().getHandle();
			if (handle == 0) return;

			boolean pressed = KeybindHelper.isPressed(ModConfig.get().getShulkerRefillKey(), handle);
			if (pressed && !wasPressed) {
				onKeyPress(client);
			}
			wasPressed = pressed;
		});
	}

	private static void onKeyPress(MinecraftClient client) {
		if (!(client.currentScreen instanceof HandledScreen<?> screen)) return;
		if (client.world == null) return;

		Slot slot = ((HandledScreenAccessor) screen).gestorage_getFocusedSlot();
		if (slot == null) return;

		ItemStack stack = slot.getStack();
		String worldKey = WorldKeyHelper.getFullWorldKey(client);
		if (worldKey == null) return;
		String slotType = getInventoryType(slot, screen);
		if (slotType == null) return;

		if (ShulkerLinkManager.isSlotLinked(worldKey, slot.getIndex(), slotType)) {
			ShulkerLinkManager.removeLink(worldKey, slot.getIndex(), slotType);
			client.player.sendMessage(Text.literal("§cLink removed."), false);
			return;
		}

		if (markedSlot == -1) {
			if (ShulkerRefillManager.isShulkerBox(stack)) {
				markedSlot = slot.getIndex();
				markedSlotType = slotType;
				client.player.sendMessage(
					Text.literal("§eShulker marked! Press key on any slot to link."),
					false
				);
			}
		} else {
			if (ShulkerRefillManager.isShulkerBox(stack)) {
				markedSlot = slot.getIndex();
				markedSlotType = slotType;
				client.player.sendMessage(
					Text.literal("§eShulker marked! Press key on any slot to link."),
					false
				);
			} else {
				ShulkerLink link = new ShulkerLink(markedSlot, markedSlotType, slot.getIndex(), slotType);
				ShulkerLinkManager.addLink(worldKey, link);
				client.player.sendMessage(
					Text.literal("§aSlots linked! Refill active."),
					false
				);
				markedSlot = -1;
				markedSlotType = "";
			}
		}
	}

	private static String getInventoryType(Slot slot, HandledScreen<?> screen) {
		if (slot.inventory instanceof net.minecraft.entity.player.PlayerInventory) return "player";

		if (screen instanceof LargeEnderScreen) {
			return "ender_large";
		}
		if (screen instanceof ExtraLargeEnderScreen) {
			return "ender_xlarge";
		}

		return null;
	}

	public static int getMarkedSlot() {
		return markedSlot;
	}

	public static String getMarkedSlotType() {
		return markedSlotType;
	}

	public static void reset() {
		markedSlot = -1;
		markedSlotType = "";
	}
}
