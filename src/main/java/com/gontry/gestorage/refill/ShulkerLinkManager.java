package com.gontry.gestorage.refill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShulkerLinkManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = Path.of("config", "gestorage_links.json");
	private static final Type MAP_TYPE = new TypeToken<Map<String, List<ShulkerLink>>>() {}.getType();

	private static Map<String, List<ShulkerLink>> allLinks = new HashMap<>();

	public static void load() {
		if (Files.exists(CONFIG_PATH)) {
			try {
				String json = Files.readString(CONFIG_PATH);
				Map<String, List<ShulkerLink>> loaded = GSON.fromJson(json, MAP_TYPE);
				if (loaded != null) {
					allLinks = loaded;
				}
			} catch (Exception e) {
				allLinks = new HashMap<>();
			}
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			Files.writeString(CONFIG_PATH, GSON.toJson(allLinks));
		} catch (IOException e) {
			// ignore
		}
	}

	public static List<ShulkerLink> getLinksForWorld(String worldKey) {
		return allLinks.getOrDefault(worldKey, new ArrayList<>());
	}

	public static void addLink(String worldKey, ShulkerLink link) {
		allLinks.computeIfAbsent(worldKey, k -> new ArrayList<>()).add(link);
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
