package com.gontry.gestorage.config;

import com.gontry.gestorage.Gestorage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CarefulBreakServerConfig {
	private static final Path CONFIG_PATH = Path.of("config", "gestorage", "careful_break.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static volatile boolean enabled = false;
	public static volatile boolean carefulBreak = false;
	public static volatile boolean carefulDrop = false;
	public static volatile boolean alwaysCareful = false;
	public static volatile boolean treeCapitator = false;
	public static volatile boolean betterHarvesting = false;
	public static volatile boolean autoReplant = false;

	private CarefulBreakServerConfig() {}

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
			enabled = getBool(json, "enabled", false);
			carefulBreak = getBool(json, "carefulBreak", false);
			carefulDrop = getBool(json, "carefulDrop", false);
			alwaysCareful = getBool(json, "alwaysCareful", false);
			treeCapitator = getBool(json, "treeCapitator", false);
			betterHarvesting = getBool(json, "betterHarvesting", false);
			autoReplant = getBool(json, "autoReplant", false);
			Gestorage.LOGGER.info("CarefulBreak server config loaded, enabled={}", enabled);
		} catch (Exception e) {
			Gestorage.LOGGER.error("Failed to load careful_break server config", e);
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			JsonObject json = new JsonObject();
			json.addProperty("version", 1);
			json.addProperty("enabled", enabled);
			json.addProperty("carefulBreak", carefulBreak);
			json.addProperty("carefulDrop", carefulDrop);
			json.addProperty("alwaysCareful", alwaysCareful);
			json.addProperty("treeCapitator", treeCapitator);
			json.addProperty("betterHarvesting", betterHarvesting);
			json.addProperty("autoReplant", autoReplant);
			Files.writeString(CONFIG_PATH, GSON.toJson(json));
		} catch (IOException e) {
			Gestorage.LOGGER.error("Failed to save careful_break config", e);
		}
	}

	private static boolean getBool(JsonObject json, String key, boolean fallback) {
		return json.has(key) && json.get(key).getAsBoolean();
	}
}
