package com.gontry.gestorage.command;

import com.gontry.gestorage.ModConstants;
import com.gontry.gestorage.ModGameRules;
import com.gontry.gestorage.network.ModNetworking;
import com.gontry.gestorage.network.ToolWheelSyncS2CPacket;
import com.gontry.gestorage.toolwheel.ToolWheelLogic;
import com.gontry.gestorage.toolwheel.ToolWheelState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

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
								.requires(source -> source.hasPermissionLevel(2))
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

									source.getWorld().getGameRules().get(ModGameRules.ENDER_CHEST_SIZE).set(mode, source.getServer());

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
							int current = ModGameRules.getEnderChestSize(source.getWorld().getGameRules());
							source.sendFeedback(() -> Text.literal("Current ender chest size: " + SIZE_MODES[current] + " (" + SIZE_VALUES[current] + " slots)"), false);
							return 1;
						})
				)
				.then(CommandManager.literal("shulkerstack")
						.then(CommandManager.argument("state", StringArgumentType.word())
								.requires(source -> source.hasPermissionLevel(2))
								.suggests((context, builder) -> {
									builder.suggest("on");
									builder.suggest("off");
									return builder.buildFuture();
								})
								.executes(context -> {
									String state = StringArgumentType.getString(context, "state").toLowerCase();
									ServerCommandSource source = context.getSource();

									Boolean value = switch (state) {
										case "on", "true" -> Boolean.TRUE;
										case "off", "false" -> Boolean.FALSE;
										default -> null;
									};

									if (value == null) {
										source.sendError(Text.literal("Invalid state: " + state + ". Use: on or off"));
										return 0;
									}

									source.getWorld().getGameRules().get(ModGameRules.STACKABLE_SHULKERS).set(value, source.getServer());

									MutableText confirm = Text.empty();
									confirm.append(Text.literal("[Gestorage] ").styled(s -> s.withColor(TextColor.fromFormatting(Formatting.GREEN))));
									confirm.append(Text.literal("Stackable shulkers set to ").styled(s -> s.withColor(TextColor.fromFormatting(Formatting.WHITE))));
									confirm.append(Text.literal(value ? "ON" : "OFF").styled(s -> s.withColor(value
											? TextColor.fromFormatting(Formatting.GREEN)
											: TextColor.fromFormatting(Formatting.RED)).withBold(true)));
									source.sendFeedback(() -> confirm, true);

									return 1;
								})
						)
						.executes(context -> {
							ServerCommandSource source = context.getSource();
							boolean current = ModGameRules.isStackableShulkers(source.getWorld().getGameRules());
							source.sendFeedback(() -> Text.literal("Stackable shulkers: " + (current ? "ON" : "OFF")), false);
							return 1;
						})
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
}
