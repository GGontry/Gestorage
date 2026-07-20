package com.gontry.gestorage.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = Path.of("config", "gestorage.json");

	private static ModConfig INSTANCE = new ModConfig();

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
		if (Files.exists(CONFIG_PATH)) {
			try {
				String json = Files.readString(CONFIG_PATH);
				INSTANCE = GSON.fromJson(json, ModConfig.class);
			} catch (Exception e) {
				INSTANCE = new ModConfig();
				save();
			}
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
		} catch (IOException e) {
		}
	}
}
