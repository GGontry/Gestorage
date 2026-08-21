package com.gontry.gestorage.client;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.network.ModNetworking;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CarefulBreakKeybinds {
	private static final Path CONFIG_PATH = Path.of("config", "gestorage", "careful_break_keybinds.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static String carefulBreakKey = "";
	public static String carefulDropKey = "";
	public static String alwaysCarefulKey = "";
	public static String treeCapitatorKey = "";
	public static String betterHarvestingKey = "";
	public static String autoReplantKey = "";
	public static String enabledKey = "";

	private static boolean prevCarefulBreak = false;
	private static boolean prevCarefulDrop = false;
	private static boolean prevAlwaysCareful = false;
	private static boolean prevTreeCapitator = false;
	private static boolean prevBetterHarvesting = false;
	private static boolean prevAutoReplant = false;
	private static boolean prevEnabled = false;

	public static void register() {
		load();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			long handle = client.getWindow() != null ? client.getWindow().getHandle() : 0;
			boolean active = client.player != null
					&& handle != 0
					&& client.currentScreen == null
					&& ClientCarefulBreakState.enabled;

			prevCarefulBreak = edge(carefulBreakKey, active, handle, prevCarefulBreak, 0);
			prevCarefulDrop = edge(carefulDropKey, active, handle, prevCarefulDrop, 1);
			prevAlwaysCareful = edge(alwaysCarefulKey, active, handle, prevAlwaysCareful, 2);
			prevTreeCapitator = edge(treeCapitatorKey, active, handle, prevTreeCapitator, 3);
			prevBetterHarvesting = edge(betterHarvestingKey, active, handle, prevBetterHarvesting, 4);
			prevAutoReplant = edge(autoReplantKey, active, handle, prevAutoReplant, 5);
			prevEnabled = edge(enabledKey, active, handle, prevEnabled, 6);
		});
	}

	private static boolean edge(String keybind, boolean active, long handle, boolean prev, int optionId) {
		boolean pressed = active && keybind != null && !keybind.isEmpty()
				&& KeybindHelper.isPressed(keybind, handle);
		if (pressed && !prev) {
			ClientPlayNetworking.send(new ModNetworking.ToggleCarefulBreakC2S(optionId));
		}
		return pressed;
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
			carefulBreakKey = getString(json, "carefulBreakKey");
			carefulDropKey = getString(json, "carefulDropKey");
			alwaysCarefulKey = getString(json, "alwaysCarefulKey");
			treeCapitatorKey = getString(json, "treeCapitatorKey");
			betterHarvestingKey = getString(json, "betterHarvestingKey");
			autoReplantKey = getString(json, "autoReplantKey");
			enabledKey = getString(json, "enabledKey");
		} catch (Exception e) {
			Gestorage.LOGGER.error("Failed to load careful_break keybinds", e);
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			JsonObject json = new JsonObject();
			json.addProperty("carefulBreakKey", carefulBreakKey);
			json.addProperty("carefulDropKey", carefulDropKey);
			json.addProperty("alwaysCarefulKey", alwaysCarefulKey);
			json.addProperty("treeCapitatorKey", treeCapitatorKey);
			json.addProperty("betterHarvestingKey", betterHarvestingKey);
			json.addProperty("autoReplantKey", autoReplantKey);
			json.addProperty("enabledKey", enabledKey);
			Files.writeString(CONFIG_PATH, GSON.toJson(json));
		} catch (IOException e) {
			Gestorage.LOGGER.error("Failed to save careful_break keybinds", e);
		}
	}

	private static String getString(JsonObject json, String key) {
		return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : "";
	}
}
