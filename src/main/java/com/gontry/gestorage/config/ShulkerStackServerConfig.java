package com.gontry.gestorage.config;

import com.gontry.gestorage.Gestorage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ShulkerStackServerConfig {
	private static final Path CONFIG_PATH = Path.of("config", "gestorage", "shulker_stack.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static volatile boolean enabled = false;

	private ShulkerStackServerConfig() {}

	public static void load() {
		try {
			if (Files.notExists(CONFIG_PATH)) {
				save();
				return;
			}
			String content = Files.readString(CONFIG_PATH);
			if (content.isBlank()) {
				save();
				return;
			}
			JsonObject json = GSON.fromJson(content, JsonObject.class);
			if (json == null) {
				save();
				return;
			}
			enabled = json.get("enabled") != null ? json.get("enabled").getAsBoolean() : false;
			Gestorage.LOGGER.info("ShulkerStack config loaded, enabled={}", enabled);
		} catch (Exception e) {
			Gestorage.LOGGER.error("Failed to load shulker_stack config", e);
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			JsonObject json = new JsonObject();
			json.addProperty("version", 1);
			json.addProperty("enabled", enabled);
			Files.writeString(CONFIG_PATH, GSON.toJson(json));
		} catch (IOException e) {
			Gestorage.LOGGER.error("Failed to save shulker_stack config", e);
		}
	}
}
