package com.gontry.gestorage.config;

import com.gontry.gestorage.Gestorage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.command.DefaultPermissions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player Careful Break settings, persisted by UUID in {@code careful_break.json}.
 *
 * <p>The packet layout is unchanged (eight booleans), but every player now owns
 * their own copy. Permission to change the copy is granted by operators with
 * {@code /gestorage careful allow <player>} or to everyone with
 * {@code /gestorage careful allowall on}. A player without permission never sees
 * effects, and their stored settings are hidden until permission is granted again.
 */
public final class CarefulBreakServerConfig {
	public static final int OPTION_CAREFUL_BREAK = 0;
	public static final int OPTION_CAREFUL_DROP = 1;
	public static final int OPTION_ALWAYS_CAREFUL = 2;
	public static final int OPTION_TREE_CAPITATOR = 3;
	public static final int OPTION_BETTER_HARVESTING = 4;
	public static final int OPTION_AUTO_REPLANT_TREES = 5;
	public static final int OPTION_AUTO_REPLANT_CROPS = 6;
	public static final int OPTION_ENABLED = 7;

	private static final int CURRENT_VERSION = 3;
	private static final Path CONFIG_PATH = Path.of("config", "gestorage", "careful_break.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Profile EMPTY = new Profile(null, false, false, false, false, false, false, false, false);

	private static final Map<UUID, Profile> PROFILES = new ConcurrentHashMap<>();
	private static final Map<UUID, String> ALLOWED = new ConcurrentHashMap<>();
	private static volatile boolean allowAll = false;

	private CarefulBreakServerConfig() {}

	/** One player's eight toggles. {@code name} is display-only and may be null. */
	public record Profile(String name, boolean enabled, boolean carefulBreak, boolean carefulDrop,
			boolean alwaysCareful, boolean treeCapitator, boolean betterHarvesting,
			boolean autoReplantTrees, boolean autoReplantCrops) {

		public boolean option(int option) {
			return switch (option) {
				case OPTION_CAREFUL_BREAK -> carefulBreak;
				case OPTION_CAREFUL_DROP -> carefulDrop;
				case OPTION_ALWAYS_CAREFUL -> alwaysCareful;
				case OPTION_TREE_CAPITATOR -> treeCapitator;
				case OPTION_BETTER_HARVESTING -> betterHarvesting;
				case OPTION_AUTO_REPLANT_TREES -> autoReplantTrees;
				case OPTION_AUTO_REPLANT_CROPS -> autoReplantCrops;
				case OPTION_ENABLED -> enabled;
				default -> false;
			};
		}

		public Profile withOption(int option, boolean value) {
			return switch (option) {
				case OPTION_CAREFUL_BREAK -> new Profile(name, enabled, value, carefulDrop, alwaysCareful,
						treeCapitator, betterHarvesting, autoReplantTrees, autoReplantCrops);
				case OPTION_CAREFUL_DROP -> new Profile(name, enabled, carefulBreak, value, alwaysCareful,
						treeCapitator, betterHarvesting, autoReplantTrees, autoReplantCrops);
				case OPTION_ALWAYS_CAREFUL -> new Profile(name, enabled, carefulBreak, carefulDrop, value,
						treeCapitator, betterHarvesting, autoReplantTrees, autoReplantCrops);
				case OPTION_TREE_CAPITATOR -> new Profile(name, enabled, carefulBreak, carefulDrop, alwaysCareful,
						value, betterHarvesting, autoReplantTrees, autoReplantCrops);
				case OPTION_BETTER_HARVESTING -> new Profile(name, enabled, carefulBreak, carefulDrop, alwaysCareful,
						treeCapitator, value, autoReplantTrees, autoReplantCrops);
				case OPTION_AUTO_REPLANT_TREES -> new Profile(name, enabled, carefulBreak, carefulDrop, alwaysCareful,
						treeCapitator, betterHarvesting, value, autoReplantCrops);
				case OPTION_AUTO_REPLANT_CROPS -> new Profile(name, enabled, carefulBreak, carefulDrop, alwaysCareful,
						treeCapitator, betterHarvesting, autoReplantTrees, value);
				case OPTION_ENABLED -> new Profile(name, value, carefulBreak, carefulDrop, alwaysCareful,
						treeCapitator, betterHarvesting, autoReplantTrees, autoReplantCrops);
				default -> this;
			};
		}

		public Profile withName(String newName) {
			if (newName == null || newName.equals(name)) return this;
			return new Profile(newName, enabled, carefulBreak, carefulDrop, alwaysCareful,
					treeCapitator, betterHarvesting, autoReplantTrees, autoReplantCrops);
		}
	}

	public static void load() {
		try {
			if (Files.notExists(CONFIG_PATH)) {
				reset();
				save();
				return;
			}
			backup();
			String content = Files.readString(CONFIG_PATH);
			if (content.isBlank()) {
				reset();
				save();
				return;
			}
			JsonObject json = GSON.fromJson(content, JsonObject.class);
			if (json == null) {
				reset();
				save();
				return;
			}
			int version = json.has("version") ? json.get("version").getAsInt() : 1;
			if (version > CURRENT_VERSION) {
				Gestorage.LOGGER.warn("CarefulBreak config version {} is newer than supported {}, resetting", version, CURRENT_VERSION);
				reset();
				save();
				return;
			}

			reset();
			allowAll = getBool(json, "allowAll", false);
			readAllowed(json.get("allowed"));
			readProfiles(json.get("players"));
			if (version < CURRENT_VERSION) {
				Gestorage.LOGGER.info("Migrated Careful Break config to per-player version {}", CURRENT_VERSION);
				save();
			}
			Gestorage.LOGGER.info("CarefulBreak server config loaded, {} player profiles, {} allowed, allowAll={}",
					PROFILES.size(), ALLOWED.size(), allowAll);
		} catch (Exception e) {
			reset();
			Gestorage.LOGGER.error("Failed to load careful_break server config", e);
		}
	}

	private static void readAllowed(JsonElement element) {
		if (element == null || !element.isJsonObject()) return;
		for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
			UUID id = parseUuid(entry.getKey());
			if (id == null) {
				Gestorage.LOGGER.warn("Skipping careful_break allow entry with invalid UUID: {}", entry.getKey());
				continue;
			}
			String name = entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : "";
			ALLOWED.put(id, name);
		}
	}

	private static void readProfiles(JsonElement element) {
		if (element == null || !element.isJsonObject()) return;
		for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
			UUID id = parseUuid(entry.getKey());
			if (id == null) {
				Gestorage.LOGGER.warn("Skipping careful_break profile with invalid UUID: {}", entry.getKey());
				continue;
			}
			if (!entry.getValue().isJsonObject()) continue;
			JsonObject profile = entry.getValue().getAsJsonObject();
			PROFILES.put(id, new Profile(
					getString(profile, "name"),
					getBool(profile, "enabled", false),
					getBool(profile, "carefulBreak", false),
					getBool(profile, "carefulDrop", false),
					getBool(profile, "alwaysCareful", false),
					getBool(profile, "treeCapitator", false),
					getBool(profile, "betterHarvesting", false),
					getBool(profile, "autoReplantTrees", false),
					getBool(profile, "autoReplantCrops", false)
			));
		}
	}

	private static void backup() {
		try {
			Path backup = CONFIG_PATH.resolveSibling("careful_break.json.backup_v" + CURRENT_VERSION);
			Files.copy(CONFIG_PATH, backup, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			Gestorage.LOGGER.warn("Failed to back up careful_break server config", e);
		}
	}

	private static void reset() {
		PROFILES.clear();
		ALLOWED.clear();
		allowAll = false;
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			JsonObject json = new JsonObject();
			json.addProperty("version", CURRENT_VERSION);
			json.addProperty("allowAll", allowAll);

			JsonObject allowed = new JsonObject();
			for (Map.Entry<UUID, String> entry : ALLOWED.entrySet()) {
				allowed.addProperty(entry.getKey().toString(), entry.getValue() == null ? "" : entry.getValue());
			}
			json.add("allowed", allowed);

			JsonObject players = new JsonObject();
			for (Map.Entry<UUID, Profile> entry : PROFILES.entrySet()) {
				Profile profile = entry.getValue();
				JsonObject object = new JsonObject();
				if (profile.name() != null) object.addProperty("name", profile.name());
				object.addProperty("enabled", profile.enabled());
				object.addProperty("carefulBreak", profile.carefulBreak());
				object.addProperty("carefulDrop", profile.carefulDrop());
				object.addProperty("alwaysCareful", profile.alwaysCareful());
				object.addProperty("treeCapitator", profile.treeCapitator());
				object.addProperty("betterHarvesting", profile.betterHarvesting());
				object.addProperty("autoReplantTrees", profile.autoReplantTrees());
				object.addProperty("autoReplantCrops", profile.autoReplantCrops());
				players.add(entry.getKey().toString(), object);
			}
			json.add("players", players);

			Files.writeString(CONFIG_PATH, GSON.toJson(json));
		} catch (IOException e) {
			Gestorage.LOGGER.error("Failed to save careful_break config", e);
		}
	}

	/** Stored profile, even for players that currently lack permission. */
	public static Profile getProfile(UUID id) {
		return PROFILES.getOrDefault(id, EMPTY);
	}

	public static Profile getProfile(ServerPlayerEntity player) {
		return getProfile(player.getUuid());
	}

	/** Profile the player may actually use right now; all off without permission. */
	public static Profile getEffectiveProfile(ServerPlayerEntity player) {
		return isPermitted(player) ? getProfile(player) : EMPTY;
	}

	public static boolean isOptionActive(ServerPlayerEntity player, int option) {
		return getEffectiveProfile(player).option(option);
	}

	/**
	 * Whether this player may change their own settings. Operators, the
	 * singleplayer host, explicitly allowed players and everyone while
	 * {@code allowAll} is on pass. Fake players and other non-player UUIDs never
	 * have a profile, so they stay unaffected.
	 */
	public static boolean isPermitted(ServerPlayerEntity player) {
		if (allowAll) return true;
		if (ALLOWED.containsKey(player.getUuid())) return true;
		if (player.getPermissions().hasPermission(DefaultPermissions.GAMEMASTERS)) return true;
		MinecraftServer server = player.getEntityWorld().getServer();
		return server != null && server.isSingleplayer() && server.isHost(player.getPlayerConfigEntry());
	}

	public static Profile toggle(ServerPlayerEntity player, int option) {
		UUID id = player.getUuid();
		Profile current = getProfile(id).withName(player.getName().getString());
		Profile updated = current.withOption(option, !current.option(option));
		PROFILES.put(id, updated);
		save();
		return updated;
	}

	/** Keeps the stored display name in sync when a player joins. */
	public static void remember(ServerPlayerEntity player) {
		UUID id = player.getUuid();
		String name = player.getName().getString();
		boolean changed = false;

		Profile profile = PROFILES.get(id);
		if (profile != null && !name.equals(profile.name())) {
			PROFILES.put(id, profile.withName(name));
			changed = true;
		}

		String allowedName = ALLOWED.get(id);
		if (allowedName != null && !allowedName.equals(name)) {
			ALLOWED.put(id, name);
			changed = true;
		}

		if (changed) save();
	}

	public static void permit(UUID id, String name) {
		ALLOWED.put(id, name == null ? "" : name);
	}

	public static void revoke(UUID id) {
		ALLOWED.remove(id);
	}

	/** Turns every option off while keeping the display name. */
	public static void clearProfile(UUID id) {
		Profile profile = PROFILES.get(id);
		if (profile != null) {
			PROFILES.put(id, EMPTY.withName(profile.name()));
		}
	}

	public static boolean isExplicitlyPermitted(UUID id) {
		return ALLOWED.containsKey(id);
	}

	public static Map<UUID, String> getAllowed() {
		return Collections.unmodifiableMap(ALLOWED);
	}

	public static boolean isAllowAll() {
		return allowAll;
	}

	public static void setAllowAll(boolean value) {
		allowAll = value;
	}

	private static UUID parseUuid(String value) {
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static boolean getBool(JsonObject json, String key, boolean fallback) {
		return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsBoolean() : fallback;
	}

	private static String getString(JsonObject json, String key) {
		return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : null;
	}
}
