package com.gontry.gestorage.client;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.config.ShulkerStackServerConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ShulkerStackKeybinds {
	private static final Path CONFIG_PATH = Path.of("config", "gestorage", "shulker_stack_keybinds.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static String toggleEnabledKey = "";
	private static boolean prevToggleEnabled = false;

	public static void register() {
		load();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			long handle = client.getWindow() != null ? client.getWindow().getHandle() : 0;
			boolean active = client.player != null
					&& handle != 0
					&& client.currentScreen == null
					&& client.isIntegratedServerRunning();

			boolean pressed = active && !toggleEnabledKey.isEmpty()
					&& KeybindHelper.isPressed(toggleEnabledKey, handle);
			if (pressed && !prevToggleEnabled) {
				ShulkerStackServerConfig.enabled = !ShulkerStackServerConfig.enabled;
				ShulkerStackServerConfig.save();
				client.player.sendMessage(Text.literal(
						"§7Stackable Shulkers: " + (ShulkerStackServerConfig.enabled ? "§aON" : "§cOFF")), true);
			}
			prevToggleEnabled = pressed;
		});
	}

	public static void load() {
		try {
			if (Files.notExists(CONFIG_PATH)) {
				save();
				return;
			}
			String content = Files.readString(CONFIG_PATH);
			if (content.isBlank()) { save(); return; }
			JsonObject json = GSON.fromJson(content, JsonObject.class);
			if (json == null) { save(); return; }
			toggleEnabledKey = json.has("toggleEnabledKey") && json.get("toggleEnabledKey").isJsonPrimitive()
					? json.get("toggleEnabledKey").getAsString() : "";
		} catch (Exception e) {
			Gestorage.LOGGER.error("Failed to load shulker_stack keybinds", e);
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			JsonObject json = new JsonObject();
			json.addProperty("toggleEnabledKey", toggleEnabledKey);
			Files.writeString(CONFIG_PATH, GSON.toJson(json));
		} catch (IOException e) {
			Gestorage.LOGGER.error("Failed to save shulker_stack keybinds", e);
		}
	}
}
