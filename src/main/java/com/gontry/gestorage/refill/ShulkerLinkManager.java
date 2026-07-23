package com.gontry.gestorage.refill;

import com.gontry.gestorage.Gestorage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShulkerLinkManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = Path.of("config", "gestorage_links.json");
	private static final Type MAP_TYPE = new TypeToken<Map<String, List<ShulkerLink>>>() {}.getType();
	private static final int CURRENT_VERSION = 1;

	private static Map<String, List<ShulkerLink>> allLinks = new HashMap<>();
	private static boolean loaded = false;

	public static void load() {
		if (loaded) {
			Gestorage.LOGGER.debug("ShulkerLinkManager already loaded, skipping");
			return;
		}
		loaded = true;

		createBackup();

		if (Files.exists(CONFIG_PATH)) {
			try {
				String json = Files.readString(CONFIG_PATH);

				JsonObject root = GSON.fromJson(json, JsonObject.class);
				if (root == null) {
					allLinks = new HashMap<>();
					return;
				}

				int version = root.has("version") ? root.get("version").getAsInt() : 0;

				if (version == 0) {
					allLinks = GSON.fromJson(json, MAP_TYPE);
					if (allLinks == null) allLinks = new HashMap<>();
					Gestorage.LOGGER.info("Loaded legacy links (version 0): {} worlds", allLinks.size());
				} else {
					if (root.has("links")) {
						allLinks = GSON.fromJson(root.get("links"), MAP_TYPE);
						if (allLinks == null) allLinks = new HashMap<>();
					} else {
						allLinks = new HashMap<>();
					}
					Gestorage.LOGGER.info("Loaded links (version {}): {} worlds", version, allLinks.size());
				}

				if (version < CURRENT_VERSION) {
					save();
					Gestorage.LOGGER.info("Migrated links from version {} to {}", version, CURRENT_VERSION);
				}
			} catch (Exception e) {
				Gestorage.LOGGER.error("Failed to load shulker links, starting fresh", e);
				allLinks = new HashMap<>();
			}
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			JsonObject root = new JsonObject();
			root.addProperty("version", CURRENT_VERSION);
			root.add("links", GSON.toJsonTree(allLinks));
			Files.writeString(CONFIG_PATH, GSON.toJson(root));
		} catch (IOException e) {
			Gestorage.LOGGER.error("Failed to save shulker links", e);
		}
	}

	private static void createBackup() {
		try {
			if (Files.exists(CONFIG_PATH)) {
				Path backup = CONFIG_PATH.getParent().resolve("gestorage_links.json.backup_v" + CURRENT_VERSION);
				Files.copy(CONFIG_PATH, backup, StandardCopyOption.REPLACE_EXISTING);
				Gestorage.LOGGER.debug("Created links backup: {}", backup.getFileName());
			}
		} catch (IOException e) {
			Gestorage.LOGGER.warn("Failed to create links backup", e);
		}
	}

	public static List<ShulkerLink> getLinksForWorld(String worldKey) {
		return allLinks.getOrDefault(worldKey, new ArrayList<>());
	}

	public static void addLink(String worldKey, ShulkerLink link) {
		List<ShulkerLink> links = allLinks.computeIfAbsent(worldKey, k -> new ArrayList<>());
		links.removeIf(l -> l.involvesSlot(link.sourceSlot(), link.sourceType()));
		links.removeIf(l -> l.involvesSlot(link.targetSlot(), link.targetType()));
		links.add(link);
		save();
	}

	public static void removeLink(String worldKey, int slot, String type) {
		List<ShulkerLink> links = allLinks.get(worldKey);
		if (links != null) {
			links.removeIf(l -> l.involvesSlot(slot, type));
			if (links.isEmpty()) {
				allLinks.remove(worldKey);
			}
			save();
		}
	}

	public static boolean isSlotLinked(String worldKey, int slot, String type) {
		return allLinks.containsKey(worldKey) &&
			allLinks.get(worldKey).stream().anyMatch(l -> l.involvesSlot(slot, type));
	}
}
