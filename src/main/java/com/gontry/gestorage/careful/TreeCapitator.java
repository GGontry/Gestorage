package com.gontry.gestorage.careful;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.config.CarefulBreakServerConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TreeCapitator {
	private static final int MAX_DISTANCE = 64;
	private static final int MAX_LOGS = 256;
	private static final int MAX_LEAVES = 512;
	private static final int LEAF_RADIUS = 3;
	private static final Map<Block, Item> LOG_TO_SAPLING = new HashMap<>();

	static {
		LOG_TO_SAPLING.put(net.minecraft.block.Blocks.OAK_LOG, Items.OAK_SAPLING);
		LOG_TO_SAPLING.put(net.minecraft.block.Blocks.BIRCH_LOG, Items.BIRCH_SAPLING);
		LOG_TO_SAPLING.put(net.minecraft.block.Blocks.SPRUCE_LOG, Items.SPRUCE_SAPLING);
		LOG_TO_SAPLING.put(net.minecraft.block.Blocks.JUNGLE_LOG, Items.JUNGLE_SAPLING);
		LOG_TO_SAPLING.put(net.minecraft.block.Blocks.ACACIA_LOG, Items.ACACIA_SAPLING);
		LOG_TO_SAPLING.put(net.minecraft.block.Blocks.DARK_OAK_LOG, Items.DARK_OAK_SAPLING);
		LOG_TO_SAPLING.put(net.minecraft.block.Blocks.MANGROVE_LOG, Items.MANGROVE_PROPAGULE);
		LOG_TO_SAPLING.put(net.minecraft.block.Blocks.CHERRY_LOG, Items.CHERRY_SAPLING);
	}

	private TreeCapitator() {}

	public static void breakTree(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state) {
		if (!state.isIn(BlockTags.LOGS)) return;

		Set<BlockPos> logs = findConnectedLogs(world, pos, state);
		Set<BlockPos> leaves = findNaturalLeaves(world, logs);
		if (leaves.isEmpty()) return;

		CarefulBreakManager.suppressBlockEffects = true;
		try {
			for (BlockPos logPos : logs) {
				if (logPos.equals(pos)) continue;
				if (!world.canPlayerModifyAt(player, logPos)) continue;
				world.breakBlock(logPos, true, player, 512);
			}

			for (BlockPos leafPos : leaves) {
				if (!world.canPlayerModifyAt(player, leafPos)) continue;
				world.breakBlock(leafPos, true, player, 512);
			}

			if (CarefulBreakServerConfig.autoReplant) {
				replantTree(player, world, logs, state);
			}
		} finally {
			CarefulBreakManager.suppressBlockEffects = false;
		}
	}

	private static void replantTree(ServerPlayerEntity player, ServerWorld world, Set<BlockPos> logs, BlockState originalState) {
		Item sapling = LOG_TO_SAPLING.get(originalState.getBlock());
		if (sapling == null) return;
		if (!(sapling instanceof BlockItem blockItem)) return;

		int lowestY = Integer.MAX_VALUE;
		for (BlockPos logPos : logs) {
			if (logPos.getY() < lowestY) lowestY = logPos.getY();
		}

		Set<BlockPos> baseLogs = new HashSet<>();
		for (BlockPos logPos : logs) {
			if (logPos.getY() == lowestY) baseLogs.add(logPos);
		}

		java.util.List<BlockPos> placements = findSaplingPlacements(world, baseLogs, lowestY);
		if (placements.isEmpty()) return;

		int needed = placements.size();
		int available = 0;
		for (int i = 0; i < player.getInventory().size(); i++) {
			if (player.getInventory().getStack(i).isOf(sapling)) {
				available += player.getInventory().getStack(i).getCount();
			}
		}
		if (available < needed) return;

		BlockState saplingState = blockItem.getBlock().getDefaultState();
		for (BlockPos placePos : placements) {
			world.setBlockState(placePos, saplingState);
		}
		player.getInventory().remove(item -> item.isOf(sapling), needed, player.getInventory());
	}

	private static java.util.List<BlockPos> findSaplingPlacements(ServerWorld world, Set<BlockPos> baseLogs, int y) {
		if (baseLogs.size() == 4) {
			int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
			for (BlockPos p : baseLogs) {
				if (p.getX() < minX) minX = p.getX();
				if (p.getZ() < minZ) minZ = p.getZ();
			}
			BlockPos[] expected = {
					new BlockPos(minX, y, minZ),
					new BlockPos(minX + 1, y, minZ),
					new BlockPos(minX, y, minZ + 1),
					new BlockPos(minX + 1, y, minZ + 1)
			};
			boolean is2x2 = true;
			for (BlockPos e : expected) {
				if (!baseLogs.contains(e)) { is2x2 = false; break; }
			}
			if (is2x2) {
				java.util.List<BlockPos> result = new java.util.ArrayList<>();
				for (BlockPos e : expected) {
					BlockPos below = e.offset(Direction.DOWN);
					if (isGoodSoil(world, below) && world.getBlockState(e).isAir()) {
						result.add(e);
					}
				}
				if (result.size() == 4) return result;
			}
		}

		for (BlockPos log : baseLogs) {
			BlockPos below = log.offset(Direction.DOWN);
			if (isGoodSoil(world, below) && world.getBlockState(log).isAir()) {
				return java.util.List.of(log);
			}
		}
		return java.util.List.of();
	}

	private static boolean isGoodSoil(ServerWorld world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.isIn(BlockTags.DIRT) 				|| state.isIn(BlockTags.BASE_STONE_OVERWORLD)
				|| state.getBlock() instanceof net.minecraft.block.FarmlandBlock
				|| state.getBlock() instanceof net.minecraft.block.GrassBlock
				|| state.getBlock() instanceof net.minecraft.block.MudBlock;
	}

	private static Set<BlockPos> findConnectedLogs(ServerWorld world, BlockPos start, BlockState startState) {
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		visited.add(start);
		Block logBlock = startState.getBlock();

		while (!queue.isEmpty()) {
			BlockPos current = queue.poll();
			for (BlockPos neighbor : BlockPos.iterate(
					new BlockPos(current.getX() - 1, current.getY() - 1, current.getZ() - 1),
					new BlockPos(current.getX() + 1, current.getY() + 1, current.getZ() + 1))) {
				if (neighbor.equals(current)) continue;
				BlockPos immutable = new BlockPos(neighbor);
				if (visited.contains(immutable)) continue;
				if (!start.isWithinDistance(immutable, MAX_DISTANCE)) continue;
				BlockState neighborState = world.getBlockState(immutable);
				if (neighborState.getBlock() == logBlock) {
					visited.add(immutable);
					if (visited.size() >= MAX_LOGS) {
						Gestorage.LOGGER.warn("[TreeCapitator] log cap ({}) reached, truncating", MAX_LOGS);
						return visited;
					}
					queue.add(immutable);
				}
			}
		}
		return visited;
	}

	private static Set<BlockPos> findNaturalLeaves(ServerWorld world, Set<BlockPos> logs) {
		Set<BlockPos> leaves = new HashSet<>();
		BooleanProperty persistent = LeavesBlock.PERSISTENT;
		for (BlockPos logPos : logs) {
			for (BlockPos leafPos : BlockPos.iterate(
					new BlockPos(logPos.getX() - LEAF_RADIUS, logPos.getY() - LEAF_RADIUS, logPos.getZ() - LEAF_RADIUS),
					new BlockPos(logPos.getX() + LEAF_RADIUS, logPos.getY() + LEAF_RADIUS, logPos.getZ() + LEAF_RADIUS))) {
				BlockState state = world.getBlockState(leafPos);
				if (state.getBlock() instanceof LeavesBlock && !state.get(persistent)) {
					leaves.add(new BlockPos(leafPos));
					if (leaves.size() >= MAX_LEAVES) {
						Gestorage.LOGGER.warn("[TreeCapitator] leaf cap ({}) reached, truncating", MAX_LEAVES);
						return leaves;
					}
				}
			}
		}
		return leaves;
	}
}
