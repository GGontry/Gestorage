package com.gontry.gestorage.shulker;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.ModGameRules;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * One-time migration from the retired {@code config/gestorage/shulker_stack.json} flag to
 * the {@code gestorage:stackableShulkers} game rule.
 *
 * <p>Runs when the server starts, before anyone joins. A legacy file that had
 * {@code enabled: true} turns the game rule on for that world; the default
 * ({@code false}) needs no migration. Both legacy files are then renamed with a
 * {@code .migrated} suffix so nothing is deleted and the migration never runs twice.
 */
public final class ShulkerStackMigration {
	private static final Path LEGACY_CONFIG = Path.of("config", "gestorage", "shulker_stack.json");
	private static final Path LEGACY_KEYBINDS = Path.of("config", "gestorage", "shulker_stack_keybinds.json");
	private static final String MIGRATED_SUFFIX = ".migrated";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private ShulkerStackMigration() {}

	public static void run(MinecraftServer server) {
		if (Files.exists(LEGACY_CONFIG)) {
			if (wasEnabled(LEGACY_CONFIG)) {
				server.getGameRules().get(ModGameRules.STACKABLE_SHULKERS).set(true, server);
				Gestorage.LOGGER.info("Migrated shulker_stack.json (enabled=true) to the gestorage:stackableShulkers game rule");
			}
			rename(LEGACY_CONFIG);
		}
		if (Files.exists(LEGACY_KEYBINDS)) {
			rename(LEGACY_KEYBINDS);
		}
	}

	private static boolean wasEnabled(Path path) {
		try {
			String content = Files.readString(path);
			if (content.isBlank()) return false;
			JsonObject json = GSON.fromJson(content, JsonObject.class);
			return json != null && json.has("enabled") && json.get("enabled").getAsBoolean();
		} catch (Exception e) {
			Gestorage.LOGGER.error("Failed to read legacy shulker_stack.json, ignoring it", e);
			return false;
		}
	}

	private static void rename(Path path) {
		Path target = path.resolveSibling(path.getFileName() + MIGRATED_SUFFIX);
		try {
			Files.move(path, target, StandardCopyOption.REPLACE_EXISTING);
			Gestorage.LOGGER.info("Renamed retired config {} to {}", path, target);
		} catch (IOException e) {
			Gestorage.LOGGER.error("Failed to rename retired config {}", path, e);
		}
	}
}
