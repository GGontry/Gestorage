package com.gontry.gestorage.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.Property;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resolves an inspector request against the server world and returns the target's
 * inventory and info lines. Kept server-side because clients do not receive
 * container contents for blocks they have not opened. The target is taken from
 * the client's vanilla crosshair raycast, which also works from a freecam camera
 * far away from the player, so no reach check is applied here: the preview is
 * read-only and the world lookup itself rejects unloaded targets.
 */
public class InspectRequestC2SPacket {
	/** Matches the client renderer's grid so info and contents never disagree. */
	private static final int MAX_SLOTS = 81;
	private static final int MAX_PROPERTIES = 8;
	/** Leave room under the packet codec's string limits. */
	private static final int MAX_TITLE_BYTES = 120;
	private static final int MAX_INFO_BYTES = 240;

	private InspectRequestC2SPacket() {}

	public static void handle(ModNetworking.InspectRequestC2S payload, ServerPlayNetworking.Context ctx) {
		ctx.server().execute(() -> {
			ServerPlayerEntity player = ctx.player();
			if (player == null) return;

			int requestId = payload.requestId();
			World world = player.getEntityWorld();
			Inspection inspection = payload.entity()
					? inspectEntity(world, payload.entityId())
					: inspectBlock(world, payload.blockPos());

			// Always answer. A silent rejection leaves the client waiting forever.
			if (inspection == null) {
				send(player, requestId, new Inspection("No target", null, ItemStack.EMPTY,
						List.of("No target")));
			} else {
				send(player, requestId, inspection);
			}
		});
	}

	private static Inspection inspectEntity(World world, int entityId) {
		Entity entity = world.getEntityById(entityId);
		if (entity == null) return null;

		// Only a few entities implement Inventory directly. Villagers, piglins,
		// pillagers and allays expose it through InventoryOwner, and players
		// (including fake players) through PlayerEntity.getInventory().
		Inventory inventory = entity instanceof Inventory inv ? inv
				: entity instanceof InventoryOwner owner ? owner.getInventory()
				: entity instanceof PlayerEntity p ? p.getInventory()
				: null;
		List<String> info = new ArrayList<>();
		String title = entity.getName().getString();
		info.add("Entity: " + title);
		info.add("Type: " + Registries.ENTITY_TYPE.getId(entity.getType()));
		info.add("Pos: " + formatPos(pos(entity)));
		if (entity instanceof LivingEntity living) {
			info.add("Health: " + (int) living.getHealth() + " / " + (int) living.getMaxHealth());
		}
		ItemStack icon = ItemStack.EMPTY;
		SpawnEggItem egg = SpawnEggItem.forEntity(entity.getType());
		if (egg != null) icon = egg.getDefaultStack();
		return new Inspection(title, inventory, icon, info);
	}

	private static Inspection inspectBlock(World world, BlockPos pos) {
		if (pos == null) return null;
		BlockState state = world.getBlockState(pos);
		if (state.isAir()) return null;

		// The pick stack respects the block's own item (and its components),
		// falling back to the plain block item for blocks without drops.
		ItemStack icon = state.getPickStack(world, pos, false);
		if (icon.isEmpty()) icon = state.getBlock().asItem().getDefaultStack();

		BlockEntity blockEntity = world.getBlockEntity(pos);
		Inventory inventory = null;
		List<String> chestInfo = List.of();
		if (state.getBlock() instanceof ChestBlock chest) {
			// true means ignoreBlocked: a block or cat above the chest must not
			// hide its contents from a read-only preview. Vanilla still returns the
			// combined 54-slot inventory from either half of a double chest.
			inventory = ChestBlock.getInventory(chest, state, world, pos, true);
			if (inventory == null && blockEntity instanceof Inventory inv) inventory = inv;
			DoubleBlockProperties.Type chestType = ChestBlock.getDoubleBlockType(state);
			if (chestType != DoubleBlockProperties.Type.SINGLE) {
				// Filled here so the info text is sent with the rest of the reply.
				chestInfo = List.of("Chest: double (" + (chestType == DoubleBlockProperties.Type.FIRST
						? "first half" : "second half") + ")");
			}
		} else if (blockEntity instanceof Inventory inv) {
			inventory = inv;
		}

		String title = blockEntity instanceof Nameable named && named.hasCustomName()
				? named.getName().getString()
				: state.getBlock().getName().getString();
		List<String> info = new ArrayList<>();
		info.addAll(chestInfo);
		info.add("Block: " + state.getBlock().getName().getString());
		info.add("ID: " + Registries.BLOCK.getId(state.getBlock()));
		info.add("Pos: " + formatPos(pos));
		info.add("Light: " + world.getLightLevel(pos));
		int shown = 0;
		for (Map.Entry<Property<?>, Comparable<?>> entry : state.getEntries().entrySet()) {
			if (shown++ >= MAX_PROPERTIES) break;
			info.add(propertyLine(entry.getKey(), entry.getValue()));
		}
		return new Inspection(title, inventory, icon, info);
	}

	private static void send(ServerPlayerEntity player, int requestId, Inspection inspection) {
		List<ItemStack> stacks = new ArrayList<>();
		Inventory inventory = inspection.inventory();
		if (inventory != null) {
			int size = Math.min(inventory.size(), MAX_SLOTS);
			for (int i = 0; i < size; i++) {
				ItemStack stack = inventory.getStack(i);
				stacks.add(stack == null ? ItemStack.EMPTY : stack.copy());
			}
		}

		List<String> info = new ArrayList<>(inspection.info().size() + 1);
		info.add("Inventory: " + (inventory == null ? "none" : inventory.size() + " slots"));
		for (String line : inspection.info()) {
			info.add(limitUtf8(line, MAX_INFO_BYTES));
		}
		ServerPlayNetworking.send(player, new ModNetworking.InspectDataS2C(requestId,
				limitUtf8(inspection.title(), MAX_TITLE_BYTES), stacks, info, inspection.icon()));
	}

	/** Trims by UTF-8 bytes so long or non-ASCII names cannot break the codec. */
	private static String limitUtf8(String value, int maxBytes) {
		if (value == null) return "";
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		if (bytes.length <= maxBytes) return value;
		String trimmed = new String(bytes, 0, maxBytes, StandardCharsets.UTF_8);
		while (trimmed.getBytes(StandardCharsets.UTF_8).length > maxBytes) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}

	private static Vec3d pos(Entity entity) {
		return new Vec3d(entity.getX(), entity.getY(), entity.getZ());
	}

	private static String formatPos(Vec3d pos) {
		return (int) Math.floor(pos.x) + ", " + (int) Math.floor(pos.y) + ", " + (int) Math.floor(pos.z);
	}

	private static String formatPos(BlockPos pos) {
		return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
	}

	private static String propertyLine(Property<?> property, Comparable<?> value) {
		return property.getName() + ": " + propertyName(property, value);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> String propertyName(Property<T> property, Comparable<?> value) {
		return property.name((T) value);
	}

	private record Inspection(String title, Inventory inventory, ItemStack icon, List<String> info) {}
}
