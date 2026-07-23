package com.gontry.gestorage.client;

import com.gontry.gestorage.Gestorage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = Path.of("config", "gestorage.json");
	private static final int CURRENT_VERSION = 1;

	private static ModConfig INSTANCE = new ModConfig();

	private int version = CURRENT_VERSION;
	private String openEnderChestKey = "";
	private String shulkerRefillKey = "";
	private int refillThreshold = 0;

	public String getOpenEnderChestKey() {
		return openEnderChestKey;
	}

	public void setOpenEnderChestKey(String key) {
		this.openEnderChestKey = key;
	}

	public String getShulkerRefillKey() {
		return shulkerRefillKey;
	}

	public void setShulkerRefillKey(String key) {
		this.shulkerRefillKey = key;
	}

	public int getRefillThreshold() {
		return refillThreshold;
	}

	public void setRefillThreshold(int threshold) {
		this.refillThreshold = threshold;
	}

	public static ModConfig get() {
		return INSTANCE;
	}

	public static void load() {
		createBackup();

		if (Files.exists(CONFIG_PATH)) {
			try {
				String json = Files.readString(CONFIG_PATH);
				JsonObject root = GSON.fromJson(json, JsonObject.class);
				if (root == null) {
					INSTANCE = new ModConfig();
					return;
				}

				int version = root.has("version") ? root.get("version").getAsInt() : 0;

				if (version == 0) {
					INSTANCE = GSON.fromJson(json, ModConfig.class);
					if (INSTANCE == null) INSTANCE = new ModConfig();
					Gestorage.LOGGER.info("Loaded legacy config (version 0)");
				} else {
					INSTANCE = GSON.fromJson(json, ModConfig.class);
					if (INSTANCE == null) INSTANCE = new ModConfig();
					Gestorage.LOGGER.info("Loaded config (version {})", version);
				}

				if (INSTANCE.version < CURRENT_VERSION) {
					INSTANCE.version = CURRENT_VERSION;
					save();
					Gestorage.LOGGER.info("Migrated config from version {} to {}", version, CURRENT_VERSION);
				}
			} catch (Exception e) {
				Gestorage.LOGGER.error("Failed to load config, using defaults", e);
				INSTANCE = new ModConfig();
				save();
			}
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			INSTANCE.version = CURRENT_VERSION;
			Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
		} catch (IOException e) {
			Gestorage.LOGGER.error("Failed to save config", e);
		}
	}

	private static void createBackup() {
		try {
			if (Files.exists(CONFIG_PATH)) {
				Path backup = CONFIG_PATH.getParent().resolve("gestorage.json.backup_v" + CURRENT_VERSION);
				Files.copy(CONFIG_PATH, backup, StandardCopyOption.REPLACE_EXISTING);
				Gestorage.LOGGER.debug("Created config backup: {}", backup.getFileName());
			}
		} catch (IOException e) {
			Gestorage.LOGGER.warn("Failed to create config backup", e);
		}
	}
}
