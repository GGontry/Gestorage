package com.gontry.gestorage.command;

import com.gontry.gestorage.ModConstants;
import com.gontry.gestorage.ModGameRules;
import com.gontry.gestorage.config.CarefulBreakServerConfig;
import com.gontry.gestorage.network.CarefulBreakStateS2CPacket;
import com.gontry.gestorage.network.EnderSizeChangedS2CPacket;
import com.gontry.gestorage.network.ModNetworking;
import com.gontry.gestorage.network.ToolWheelSyncS2CPacket;
import com.gontry.gestorage.toolwheel.ToolWheelLogic;
import com.gontry.gestorage.toolwheel.ToolWheelState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class GestorageCommands {
	private static final String[] SIZE_MODES = {"normal", "large", "extra_large"};
	private static final int[] SIZE_VALUES = {ModConstants.NORMAL_ENDER_SIZE, ModConstants.LARGE_ENDER_SIZE, ModConstants.EXTRA_LARGE_ENDER_SIZE};

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			registerCommands(dispatcher);
		});
	}

	private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("gestorage")
				.then(CommandManager.literal("config")
						.executes(context -> {
							ServerCommandSource source = context.getSource();
							ServerPlayerEntity player = source.getPlayer();
							if (player == null) {
								source.sendError(Text.literal("[Gestorage] This command must be run by a player"));
								return 0;
							}
							ServerPlayNetworking.send(player, new ModNetworking.OpenConfigScreenS2C());
							source.sendFeedback(() -> Text.literal("[Gestorage] Opening config screen..."), false);
							return 1;
						})
				)
				.then(CommandManager.literal("endersize")
						.then(CommandManager.argument("size", StringArgumentType.word())
								.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
								.suggests((context, builder) -> {
									for (String mode : SIZE_MODES) {
										builder.suggest(mode);
									}
									return builder.buildFuture();
								})
								.executes(context -> {
									String size = StringArgumentType.getString(context, "size");
									ServerCommandSource source = context.getSource();

									int mode = switch (size) {
										case "normal" -> ModConstants.MODE_NORMAL;
										case "large" -> ModConstants.MODE_LARGE;
										case "extra_large" -> ModConstants.MODE_EXTRA_LARGE;
										default -> -1;
									};

									if (mode == -1) {
										source.sendError(Text.literal("Invalid size mode: " + size + ". Use: normal, large, or extra_large"));
										return 0;
									}

									ModGameRules.getGlobalRules(source.getServer()).setValue(ModGameRules.ENDER_CHEST_SIZE, mode, source.getServer());
									EnderSizeChangedS2CPacket.broadcast(source.getServer());

									MutableText confirm = Text.empty();
									confirm.append(Text.literal("[Gestorage] ").styled(s -> s.withColor(TextColor.fromFormatting(Formatting.GREEN))));
									confirm.append(Text.literal("Ender chest size set to ").styled(s -> s.withColor(TextColor.fromFormatting(Formatting.WHITE))));
									confirm.append(Text.literal(size.toUpperCase()).styled(s -> s.withColor(TextColor.fromFormatting(Formatting.YELLOW)).withBold(true)));
									confirm.append(Text.literal(" (" + SIZE_VALUES[mode] + " slots)").styled(s -> s.withColor(TextColor.fromFormatting(Formatting.GRAY))));
									source.sendFeedback(() -> confirm, true);

									if (mode == ModConstants.MODE_EXTRA_LARGE) {
										source.sendFeedback(() -> {
											MutableText warning = Text.empty();
											warning.append(Text.literal("[Gestorage] ").styled(s -> s.withColor(TextColor.fromFormatting(Formatting.YELLOW))));
											warning.append(Text.literal("WARNING: Extra Large mode (" + ModConstants.EXTRA_LARGE_ENDER_SIZE + " slots) is experimental. ").styled(s -> s.withColor(TextColor.fromFormatting(Formatting.RED))));
											warning.append(Text.literal("It may cause lag or crashes with large amounts of shulker box data. Use at your own risk!").styled(s -> s.withColor(TextColor.fromFormatting(Formatting.RED))));
											return warning;
										}, false);
									}

									return 1;
								})
						)
						.executes(context -> {
							ServerCommandSource source = context.getSource();
							int current = ModGameRules.getEnderChestSize(ModGameRules.getGlobalRules(source.getServer()));
							source.sendFeedback(() -> Text.literal("Current ender chest size: " + SIZE_MODES[current] + " (" + SIZE_VALUES[current] + " slots)"), false);
							return 1;
						})
				)
				.then(CommandManager.literal("careful")
						.then(CommandManager.literal("allow")
								.then(CommandManager.argument("targets", GameProfileArgumentType.gameProfile())
										.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
										.executes(context -> setCarefulPermission(context, true))))
						.then(CommandManager.literal("deny")
								.then(CommandManager.argument("targets", GameProfileArgumentType.gameProfile())
										.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
										.executes(context -> setCarefulPermission(context, false))))
						.then(CommandManager.literal("allowall")
								.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
								.executes(context -> reportCarefulAllowAll(context.getSource()))
								.then(CommandManager.literal("on")
										.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
										.executes(context -> setCarefulAllowAll(context.getSource(), true)))
								.then(CommandManager.literal("off")
										.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
										.executes(context -> setCarefulAllowAll(context.getSource(), false))))
						.then(CommandManager.literal("list")
								.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
								.executes(context -> listCarefulPermissions(context.getSource())))
				)
				.then(CommandManager.literal("shulkerstack")
						.then(CommandManager.literal("players")
								.executes(context -> reportPlayersStack(context.getSource()))
								.then(CommandManager.literal("on")
										.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
										.executes(context -> setPlayersStack(context.getSource(), true)))
								.then(CommandManager.literal("off")
										.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
										.executes(context -> setPlayersStack(context.getSource(), false))))
						.then(CommandManager.literal("hoppers")
								.executes(context -> reportHoppersStack(context.getSource()))
								.then(CommandManager.literal("on")
										.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
										.executes(context -> setHoppersStack(context.getSource(), true)))
								.then(CommandManager.literal("off")
										.requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
										.executes(context -> setHoppersStack(context.getSource(), false))))
						.executes(context -> reportShulkerStack(context.getSource()))
				)
				.then(CommandManager.literal("toolslot")
						.executes(context -> {
							ServerCommandSource source = context.getSource();
							ServerPlayerEntity player = source.getPlayer();
							if (player == null) {
								source.sendError(Text.literal("[Gestorage] This command must be run by a player"));
								return 0;
							}
							ToolWheelState state = ToolWheelState.getExisting(player);
							int current = state == null ? ModConstants.TOOL_SLOT_NONE : state.defaultSlot;
							source.sendFeedback(() -> Text.literal("Current Auto Tool slot: "
									+ (current == ModConstants.TOOL_SLOT_NONE ? "selected slot" : String.valueOf(current + 1))), false);
							return 1;
						})
						.then(CommandManager.literal("clear")
								.executes(context -> {
									ServerCommandSource source = context.getSource();
									ServerPlayerEntity player = source.getPlayer();
									if (player == null) {
										source.sendError(Text.literal("[Gestorage] This command must be run by a player"));
										return 0;
									}
									ToolWheelLogic.setDefaultSlot(player, ModConstants.TOOL_SLOT_NONE);
									ToolWheelSyncS2CPacket.sendTo(player);
									source.sendFeedback(() -> Text.literal("[Gestorage] Auto Tool slot cleared (follows the selected slot)"), true);
									return 1;
								})
						)
						.then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 9))
								.executes(context -> {
									ServerCommandSource source = context.getSource();
									ServerPlayerEntity player = source.getPlayer();
									if (player == null) {
										source.sendError(Text.literal("[Gestorage] This command must be run by a player"));
										return 0;
									}
									int slot = IntegerArgumentType.getInteger(context, "slot") - 1;
									ToolWheelLogic.setDefaultSlot(player, slot);
									ToolWheelSyncS2CPacket.sendTo(player);
									source.sendFeedback(() -> Text.literal("[Gestorage] Auto Tool slot set to " + (slot + 1)), true);
									return 1;
								})
						)
				)
		);
	}

	private static int setCarefulPermission(CommandContext<ServerCommandSource> context, boolean allow)
			throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		Collection<PlayerConfigEntry> targets = GameProfileArgumentType.getProfileArgument(context, "targets");
		List<String> names = new ArrayList<>();

		for (PlayerConfigEntry target : targets) {
			if (allow) {
				CarefulBreakServerConfig.permit(target.id(), target.name());
			} else {
				CarefulBreakServerConfig.revoke(target.id());
				CarefulBreakServerConfig.clearProfile(target.id());
			}
			names.add(target.name());

			ServerPlayerEntity online = source.getServer().getPlayerManager().getPlayer(target.id());
			if (online != null) {
				CarefulBreakStateS2CPacket.sendTo(online);
				online.sendMessage(Text.literal(allow
						? "§a[Gestorage] You can now enable Careful Break options yourself."
						: "§c[Gestorage] Your Careful Break options are now managed by an operator."), false);
			}
		}

		CarefulBreakServerConfig.save();
		String label = allow ? "Allowed" : "Denied";
		String list = String.join(", ", names);
		source.sendFeedback(() -> Text.literal("[Gestorage] " + label + " Careful Break for " + list), true);
		return 1;
	}

	private static int reportCarefulAllowAll(ServerCommandSource source) {
		source.sendFeedback(() -> Text.literal("[Gestorage] Everyone can change their own Careful Break settings: "
				+ state(CarefulBreakServerConfig.isAllowAll())
				+ " (operators, the host and explicitly allowed players can always do it)"), false);
		return 1;
	}

	private static int setCarefulAllowAll(ServerCommandSource source, boolean value) {
		CarefulBreakServerConfig.setAllowAll(value);
		CarefulBreakServerConfig.save();
		// Permission just changed for everyone; refresh each client's effective state.
		CarefulBreakStateS2CPacket.broadcast(source.getServer());
		source.sendFeedback(() -> Text.literal("[Gestorage] Everyone can change their own Careful Break settings: "
				+ state(value)), true);
		return 1;
	}

	private static int listCarefulPermissions(ServerCommandSource source) {
		source.sendFeedback(() -> Text.literal("[Gestorage] Everyone allowed: "
				+ state(CarefulBreakServerConfig.isAllowAll())), false);

		var allowed = CarefulBreakServerConfig.getAllowed();
		if (allowed.isEmpty()) {
			source.sendFeedback(() -> Text.literal("[Gestorage] Allowed players: none "
					+ "(operators, the host and explicitly allowed players can always toggle)"), false);
			return 1;
		}

		String names = allowed.values().stream()
				.map(name -> name == null || name.isBlank() ? "?" : name)
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.collect(Collectors.joining(", "));
		source.sendFeedback(() -> Text.literal("[Gestorage] Allowed players: " + names), false);
		return 1;
	}

	private static int reportShulkerStack(ServerCommandSource source) {
		boolean players = ModGameRules.isStackableShulkers(ModGameRules.getGlobalRules(source.getServer()));
		boolean hoppers = ModGameRules.isHopperStackableShulkers(ModGameRules.getGlobalRules(source.getServer()));
		source.sendFeedback(() -> Text.literal("[Gestorage] Players: " + state(players)
				+ " | Hoppers: " + hopperState(players, hoppers)), false);
		if (!hoppers) {
			source.sendFeedback(() -> Text.literal("[Gestorage] Hoppers keep empty shulkers at 1. "
					+ "Use /gestorage shulkerstack hoppers on to let them stack."), false);
		} else if (!players) {
			source.sendFeedback(() -> Text.literal("[Gestorage] Hoppers are enabled but inactive while players is OFF. "
					+ "Use /gestorage shulkerstack players on."), false);
		}
		return 1;
	}

	private static int reportPlayersStack(ServerCommandSource source) {
		boolean players = ModGameRules.isStackableShulkers(ModGameRules.getGlobalRules(source.getServer()));
		boolean hoppers = ModGameRules.isHopperStackableShulkers(ModGameRules.getGlobalRules(source.getServer()));
		source.sendFeedback(() -> Text.literal("[Gestorage] Players can stack empty shulkers: " + state(players)
				+ " | Hoppers: " + hopperState(players, hoppers)), false);
		return 1;
	}

	private static int reportHoppersStack(ServerCommandSource source) {
		boolean players = ModGameRules.isStackableShulkers(ModGameRules.getGlobalRules(source.getServer()));
		boolean hoppers = ModGameRules.isHopperStackableShulkers(ModGameRules.getGlobalRules(source.getServer()));
		source.sendFeedback(() -> Text.literal("[Gestorage] Hoppers can stack empty shulkers: " + hopperState(players, hoppers)
				+ " | Players: " + state(players)), false);
		if (!hoppers) {
			source.sendFeedback(() -> Text.literal("[Gestorage] Hoppers keep empty shulkers at 1. "
					+ "Use /gestorage shulkerstack hoppers on to let them stack."), false);
		} else if (!players) {
			source.sendFeedback(() -> Text.literal("[Gestorage] Hoppers are enabled but inactive while players is OFF. "
					+ "Use /gestorage shulkerstack players on."), false);
		}
		return 1;
	}

	private static int setPlayersStack(ServerCommandSource source, boolean value) {
		ModGameRules.getGlobalRules(source.getServer()).setValue(ModGameRules.STACKABLE_SHULKERS, value, source.getServer());
		boolean hoppers = ModGameRules.isHopperStackableShulkers(ModGameRules.getGlobalRules(source.getServer()));

		MutableText confirm = Text.empty();
		confirm.append(Text.literal("[Gestorage] ").styled(s -> s.withColor(TextColor.fromFormatting(Formatting.GREEN))));
		confirm.append(Text.literal(value ? "Players can stack empty shulkers" : "Empty shulker stacking disabled for everyone")
				.styled(s -> s.withColor(TextColor.fromFormatting(Formatting.WHITE))));
		confirm.append(Text.literal(value
				? (hoppers ? " (hoppers: ON)" : " (hoppers: OFF)")
				: " (hopper setting kept, but inactive)")
				.styled(s -> s.withColor(TextColor.fromFormatting(Formatting.GRAY))));
		source.sendFeedback(() -> confirm, true);

		if (value && !hoppers) {
			source.sendFeedback(() -> Text.literal("[Gestorage] Hoppers still keep empty shulkers at 1. "
					+ "Use /gestorage shulkerstack hoppers on to let them stack."), false);
		}
		return 1;
	}

	private static int setHoppersStack(ServerCommandSource source, boolean value) {
		var rules = ModGameRules.getGlobalRules(source.getServer());
		boolean players = ModGameRules.isStackableShulkers(rules);
		rules.setValue(ModGameRules.HOPPER_STACKABLE_SHULKERS, value, source.getServer());

		MutableText confirm = Text.empty();
		confirm.append(Text.literal("[Gestorage] ").styled(s -> s.withColor(TextColor.fromFormatting(Formatting.GREEN))));
		confirm.append(Text.literal(value ? "Hoppers can stack empty shulkers" : "Hoppers keep empty shulkers at 1")
				.styled(s -> s.withColor(TextColor.fromFormatting(Formatting.WHITE))));
		if (value && !players) {
			// The main rule gates item max counts for everyone; enabling the hopper
			// rule alone would otherwise silently do nothing.
			rules.setValue(ModGameRules.STACKABLE_SHULKERS, true, source.getServer());
			confirm.append(Text.literal(" (players stacking was also enabled, as it is required)")
					.styled(s -> s.withColor(TextColor.fromFormatting(Formatting.GRAY))));
		} else {
			confirm.append(Text.literal(value ? " (players: ON)" : " (only players stack)")
					.styled(s -> s.withColor(TextColor.fromFormatting(Formatting.GRAY))));
		}
		source.sendFeedback(() -> confirm, true);
		return 1;
	}

	private static String state(boolean value) {
		return value ? "ON" : "OFF";
	}

	private static String hopperState(boolean players, boolean hoppers) {
		if (!hoppers) return "OFF";
		return players ? "ON" : "ON (inactive)";
	}
}
