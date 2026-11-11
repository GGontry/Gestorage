package com.gontry.gestorage;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRules;

/**
 * One-time import of the game rule values persisted by the 1.21.1 line.
 *
 * <p>1.21.11 made game rules registry entries keyed by
 * {@link net.minecraft.util.Identifier} and renamed every vanilla rule to snake_case;
 * identifier paths cannot contain uppercase letters, so
 * {@code gestorage:enderChestSize}, {@code gestorage:stackableShulkers} and
 * {@code gestorage:hopperStackableShulkers} can no longer be registered (vanilla
 * shipped {@code GameRuleRegistryFix} for exactly the same reason). The rules are
 * registered as {@code gestorage:ender_chest_size},
 * {@code gestorage:stackable_shulkers} and
 * {@code gestorage:hopper_stackable_shulkers} instead.
 *
 * <p>While a world loads, vanilla parses the old keys with {@code resultOrPartial},
 * so their values are logged and dropped instead of crashing. The import therefore
 * captures them from the raw level data with a mixin on
 * {@code LevelInfo.fromDynamic} (before the codec runs), and applies them to the new
 * rules once the server is up. A marker state makes it run once per world, so a later
 * {@code /gamerule} change is never overwritten.
 */
public final class GameRuleNameMigration {
	private static final String MARKER_KEY = "gestorage_rule_name_migration";
	private static final PersistentStateType<Marker> MARKER_TYPE = new PersistentStateType<>(
			MARKER_KEY, Marker::new, MapCodec.unitCodec(Marker::new), DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	/** Values read from the level data while it loads, or {@code null} when there is nothing to import. */
	private static volatile LegacyValues captured;

	private GameRuleNameMigration() {}

	/**
	 * Reads the legacy rule values from the level data before the game rule codec drops
	 * them. Called for every level file that is parsed (including the world list), so
	 * the value is always overwritten by the world that is actually being opened.
	 */
	public static void captureFromLevel(Dynamic<?> levelData) {
		captured = null;
		// Pre-1.21.11 worlds store the compound under "GameRules"; vanilla's datafixer
		// renames it to "game_rules" in memory but leaves the unknown keys in place.
		// Values were strings, the same shape vanilla's own GameRuleRegistryFix reads.
		OptionalDynamic<?> legacy = levelData.get("GameRules").result().isPresent()
				? levelData.get("GameRules")
				: levelData.get("game_rules");
		if (legacy.result().isEmpty()) return;

		Integer enderChestSize = null;
		OptionalDynamic<?> size = legacy.get("gestorage:enderChestSize");
		if (size.result().isPresent()) {
			try {
				enderChestSize = Integer.parseInt(size.asString(""));
			} catch (NumberFormatException e) {
				Gestorage.LOGGER.warn("Ignoring invalid legacy gestorage:enderChestSize value");
			}
		}
		Boolean stackableShulkers = readBoolean(legacy, "gestorage:stackableShulkers");
		Boolean hopperStackableShulkers = readBoolean(legacy, "gestorage:hopperStackableShulkers");
		if (enderChestSize == null && stackableShulkers == null && hopperStackableShulkers == null) return;

		captured = new LegacyValues(enderChestSize, stackableShulkers, hopperStackableShulkers);
	}

	/** Applies the values captured while the level loaded to the running overworld, once per world. */
	public static void apply(MinecraftServer server) {
		LegacyValues legacy = captured;
		captured = null;
		if (legacy == null) return;

		PersistentStateManager stateManager = server.getOverworld().getPersistentStateManager();
		if (stateManager.get(MARKER_TYPE) != null) return;
		// Marks the world as migrated even when every value was invalid, so the level
		// data is only read once per world.
		stateManager.getOrCreate(MARKER_TYPE);

		GameRules rules = server.getOverworld().getGameRules();
		if (legacy.enderChestSize() != null) {
			int value = legacy.enderChestSize();
			if (value >= ModConstants.MODE_NORMAL && value <= ModConstants.MODE_EXTRA_LARGE) {
				rules.setValue(ModGameRules.ENDER_CHEST_SIZE, value, server);
				Gestorage.LOGGER.info("Migrated gestorage:enderChestSize={} to gestorage:ender_chest_size", value);
			} else {
				Gestorage.LOGGER.warn("Ignoring out-of-range legacy gestorage:enderChestSize value {}", value);
			}
		}
		if (legacy.stackableShulkers() != null) {
			applyBoolean(legacy.stackableShulkers(), "gestorage:stackableShulkers",
					ModGameRules.STACKABLE_SHULKERS, rules, server);
		}
		if (legacy.hopperStackableShulkers() != null) {
			applyBoolean(legacy.hopperStackableShulkers(), "gestorage:hopperStackableShulkers",
					ModGameRules.HOPPER_STACKABLE_SHULKERS, rules, server);
		}
	}

	private static Boolean readBoolean(OptionalDynamic<?> legacy, String key) {
		OptionalDynamic<?> value = legacy.get(key);
		if (value.result().isEmpty()) return null;
		return Boolean.parseBoolean(value.asString("false"));
	}

	private static void applyBoolean(boolean value, String oldKey, GameRule<Boolean> rule,
			GameRules rules, MinecraftServer server) {
		rules.setValue(rule, value, server);
		Gestorage.LOGGER.info("Migrated {}={} to {}", oldKey, value, rule.getId());
	}

	/** Legacy values read from the level data; {@code null} fields mean the key was absent or invalid. */
	private record LegacyValues(Integer enderChestSize, Boolean stackableShulkers, Boolean hopperStackableShulkers) {
	}

	/** Empty marker state; its presence in {@code world/data} means the migration already ran. */
	private static final class Marker extends PersistentState {
	}
}
