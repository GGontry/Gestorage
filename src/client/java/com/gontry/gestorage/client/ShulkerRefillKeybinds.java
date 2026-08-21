package com.gontry.gestorage.client;

import com.gontry.gestorage.client.config.ModuleConfig;
import com.gontry.gestorage.mixin.HandledScreenAccessor;
import com.gontry.gestorage.refill.ShulkerLink;
import com.gontry.gestorage.refill.ShulkerLinkManager;
import com.gontry.gestorage.refill.ShulkerRefillManager;
import com.gontry.gestorage.screen.InventoryTypeProvider;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class ShulkerRefillKeybinds {
	private static int markedSlot = -1;
	private static String markedSlotType = "";
	private static boolean wasPressed = false;
	private static boolean wasTogglePressed = false;

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			long handle = client.getWindow() != null ? client.getWindow().getHandle() : 0;
			boolean inGame = client.player != null
					&& client.currentScreen == null
					&& handle != 0;

			boolean togglePressed = inGame && !ModuleConfig.shulkerRefill().toggleEnabledKey().isEmpty()
					&& KeybindHelper.isPressed(ModuleConfig.shulkerRefill().toggleEnabledKey(), handle);
			if (togglePressed && !wasTogglePressed) {
				boolean newState = !ModuleConfig.shulkerRefill().enabled();
				ModuleConfig.shulkerRefill().enabled(newState);
				ModuleConfig.shulkerRefill().save();
				client.player.sendMessage(Text.literal(
						"§7Shulker Restock: " + (newState ? "§aON" : "§cOFF")), true);
			}
			wasTogglePressed = togglePressed;

			if (!ModuleConfig.shulkerRefill().enabled()) { wasPressed = false; return; }
			if (client.player == null) { wasPressed = false; return; }

			long h = client.getWindow() != null ? client.getWindow().getHandle() : 0;
			if (h == 0) { wasPressed = false; return; }

			if (markedSlot >= 0 && !(client.currentScreen instanceof HandledScreen<?>)) {
				reset();
			}

			if (markedSlot >= 0 && GLFW.glfwGetKey(h, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
				reset();
				if (client.player != null) {
					client.player.sendMessage(Text.literal("§cShulker marking cancelled."), true);
				}
				wasPressed = false;
				return;
			}

			boolean pressed = KeybindHelper.isPressed(ModuleConfig.shulkerRefill().shulkerRefillKey(), h);
			if (pressed && !wasPressed && client.currentScreen instanceof HandledScreen<?>) {
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
			client.player.sendMessage(Text.literal("§cLink removed."), true);
			return;
		}

		if (markedSlot == -1) {
			if (ShulkerRefillManager.isShulkerBox(stack)) {
				markedSlot = slot.getIndex();
				markedSlotType = slotType;
				client.player.sendMessage(
					Text.literal("§eShulker marked! Press key on any slot to link."),
					true
				);
			}
		} else {
			if (ShulkerRefillManager.isShulkerBox(stack)) {
				markedSlot = slot.getIndex();
				markedSlotType = slotType;
				client.player.sendMessage(
					Text.literal("§eShulker marked! Press key on any slot to link."),
					true
				);
			} else {
				ShulkerLink link = new ShulkerLink(markedSlot, markedSlotType, slot.getIndex(), slotType);
				ShulkerLinkManager.addLink(worldKey, link);
				client.player.sendMessage(
					Text.literal("§aSlots linked! Refill active."),
					true
				);
				markedSlot = -1;
				markedSlotType = "";
			}
		}
	}

	private static String getInventoryType(Slot slot, HandledScreen<?> screen) {
		if (slot.inventory instanceof net.minecraft.entity.player.PlayerInventory) return "player";
		if (slot.inventory instanceof net.minecraft.inventory.EnderChestInventory) return "ender_normal";

		if (screen instanceof InventoryTypeProvider provider) {
			return provider.getInventoryType();
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
