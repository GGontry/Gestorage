package com.gontry.gestorage;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRuleCategory;
import net.minecraft.world.rule.GameRuleType;
import net.minecraft.world.rule.GameRuleVisitor;
import net.minecraft.world.rule.GameRules;

/**
 * Registers Gestorage's game rules into the vanilla {@link Registries#GAME_RULE}
 * registry, mirroring how vanilla registers its own rules (the Fabric gamerule
 * helper module was removed in 1.21.11).
 *
 * <p>Rules are registered with {@link FeatureSet#empty()} so they exist in every
 * world regardless of enabled feature flags.
 *
 * <p>1.21.11 renamed every vanilla rule to snake_case and made rule ids
 * {@link Identifier identifiers}, whose paths cannot contain uppercase letters.
 * The 1.21.1 names ({@code gestorage:enderChestSize} and friends) therefore can no
 * longer be registered, exactly like vanilla's own {@code GameRuleRegistryFix};
 * {@link GameRuleNameMigration} imports the values persisted under the old names.
 */
public class ModGameRules {
	public static GameRule<Integer> ENDER_CHEST_SIZE;
	public static GameRule<Boolean> STACKABLE_SHULKERS;
	public static GameRule<Boolean> HOPPER_STACKABLE_SHULKERS;

	public static void register() {
		ENDER_CHEST_SIZE = registerIntRule("ender_chest_size",
				ModConstants.MODE_NORMAL, ModConstants.MODE_EXTRA_LARGE, ModConstants.MODE_NORMAL);
		STACKABLE_SHULKERS = registerBooleanRule("stackable_shulkers", false);
		HOPPER_STACKABLE_SHULKERS = registerBooleanRule("hopper_stackable_shulkers", false);
	}

	/** Registers a boolean rule in the vanilla MISC category. */
	private static GameRule<Boolean> registerBooleanRule(String name, boolean defaultValue) {
		return Registry.register(Registries.GAME_RULE, Identifier.of(Gestorage.MOD_ID, name),
				new GameRule<>(GameRuleCategory.MISC, GameRuleType.BOOL, BoolArgumentType.bool(),
						GameRuleVisitor::visitBoolean, Codec.BOOL, value -> value ? 1 : 0,
						defaultValue, FeatureSet.empty()));
	}

	/** Registers an int rule in the vanilla MISC category, bounded like the old Fabric factory. */
	private static GameRule<Integer> registerIntRule(String name, int minValue, int maxValue, int defaultValue) {
		return Registry.register(Registries.GAME_RULE, Identifier.of(Gestorage.MOD_ID, name),
				new GameRule<>(GameRuleCategory.MISC, GameRuleType.INT, IntegerArgumentType.integer(minValue, maxValue),
						GameRuleVisitor::visitInt, Codec.intRange(minValue, maxValue), value -> value,
						defaultValue, FeatureSet.empty()));
	}

	/**
	 * The rules Gestorage treats as server-wide. 1.21.11 stores game rules per
	 * world, but Gestorage's settings keep their 1.21.1 meaning, so every read and
	 * write resolves to the overworld regardless of the dimension the player is in.
	 */
	public static GameRules getGlobalRules(MinecraftServer server) {
		return server.getOverworld().getGameRules();
	}

	public static int getEnderChestSize(GameRules gameRules) {
		int raw = gameRules.getValue(ENDER_CHEST_SIZE);
		if (raw < ModConstants.MODE_NORMAL || raw > ModConstants.MODE_EXTRA_LARGE) {
			Gestorage.LOGGER.warn("EnderChestSize GameRule has invalid value {}, clamping to MODE_NORMAL", raw);
			return ModConstants.MODE_NORMAL;
		}
		return raw;
	}

	public static boolean isStackableShulkers(GameRules gameRules) {
		return gameRules.getValue(STACKABLE_SHULKERS);
	}

	public static boolean isHopperStackableShulkers(GameRules gameRules) {
		return gameRules.getValue(HOPPER_STACKABLE_SHULKERS);
	}
}
