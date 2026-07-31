package com.gontry.gestorage.client.config;

import com.gontry.gestorage.Gestorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ModuleConfig {
	private static EnderChestConfig ENDER_CHEST;
	private static ShulkerRefillConfig SHULKER_REFILL;

	private ModuleConfig() {}

	public static void initialize() {
		createBackup(Path.of("config", "gestorage", "ender_chest.json"));
		createBackup(Path.of("config", "gestorage", "shulker_refill.json"));
		ENDER_CHEST = EnderChestConfig.createAndLoad();
		SHULKER_REFILL = ShulkerRefillConfig.createAndLoad();
		Gestorage.LOGGER.info("Module configs initialized");
	}

	public static EnderChestConfig enderChest() {
		return ENDER_CHEST;
	}

	public static ShulkerRefillConfig shulkerRefill() {
		return SHULKER_REFILL;
	}

	static void createBackup(Path configPath) {
		try {
			if (Files.exists(configPath)) {
				Path backup = configPath.getParent().resolve(configPath.getFileName() + ".backup");
				Files.copy(configPath, backup, StandardCopyOption.REPLACE_EXISTING);
				Gestorage.LOGGER.debug("Created config backup: {}", backup.getFileName());
			}
		} catch (IOException e) {
			Gestorage.LOGGER.warn("Failed to create config backup for {}", configPath, e);
		}
	}
}
