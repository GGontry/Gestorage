package com.gontry.gestorage;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.GameRules;

public class ModGameRules {
	public static GameRules.Key<GameRules.IntRule> ENDER_CHEST_SIZE;
	public static GameRules.Key<GameRules.BooleanRule> STACKABLE_SHULKERS;

	public static void register() {
		ENDER_CHEST_SIZE = GameRuleRegistry.register(
				"gestorage:enderChestSize",
				GameRules.Category.MISC,
				GameRuleFactory.createIntRule(0, 0, 2)
		);
		STACKABLE_SHULKERS = GameRuleRegistry.register(
				"gestorage:stackableShulkers",
				GameRules.Category.MISC,
				GameRuleFactory.createBooleanRule(false)
		);
	}

	public static int getEnderChestSize(GameRules gameRules) {
		int raw = gameRules.get(ENDER_CHEST_SIZE).get();
		if (raw < ModConstants.MODE_NORMAL || raw > ModConstants.MODE_EXTRA_LARGE) {
			Gestorage.LOGGER.warn("EnderChestSize GameRule has invalid value {}, clamping to MODE_NORMAL", raw);
			return ModConstants.MODE_NORMAL;
		}
		return raw;
	}

	public static boolean isStackableShulkers(GameRules gameRules) {
		return gameRules.getBoolean(STACKABLE_SHULKERS);
	}
}
