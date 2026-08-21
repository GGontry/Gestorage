package com.gontry.gestorage.careful;

import com.gontry.gestorage.config.CarefulBreakServerConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BambooBlock;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.KelpBlock;
import net.minecraft.block.KelpPlantBlock;
import net.minecraft.block.SugarCaneBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class BetterHarvesting {
	private static final int MAX_DISTANCE = 32;
	private static final Map<Block, Item> CROP_TO_ITEM = new HashMap<>();

	static {
		CROP_TO_ITEM.put(net.minecraft.block.Blocks.BAMBOO, Items.BAMBOO);
		CROP_TO_ITEM.put(net.minecraft.block.Blocks.SUGAR_CANE, Items.SUGAR_CANE);
		CROP_TO_ITEM.put(net.minecraft.block.Blocks.CACTUS, Items.CACTUS);
		CROP_TO_ITEM.put(net.minecraft.block.Blocks.KELP, Items.KELP);
		CROP_TO_ITEM.put(net.minecraft.block.Blocks.KELP_PLANT, Items.KELP);
	}

	private BetterHarvesting() {}

	public static void breakCrops(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state) {
		Block block = state.getBlock();
		if (!isSupportedCrop(block)) {
			return;
		}

		Set<BlockPos> crops = findConnectedCrops(world, pos, block);

		CarefulBreakManager.suppressBlockEffects = true;
		try {
			int broken = 0;
			for (BlockPos cropPos : crops) {
				if (cropPos.equals(pos)) continue;
				if (!world.canPlayerModifyAt(player, cropPos)) continue;
				world.breakBlock(cropPos, true, player, 512);
				broken++;
			}

			if (CarefulBreakServerConfig.autoReplant) {
				replantCrop(player, world, crops, block);
			}
		} finally {
			CarefulBreakManager.suppressBlockEffects = false;
		}
	}

	private static void replantCrop(ServerPlayerEntity player, ServerWorld world, Set<BlockPos> crops, Block cropBlock) {
		Item seed = CROP_TO_ITEM.get(cropBlock);
		if (seed == null) return;

		BlockPos lowest = null;
		for (BlockPos cropPos : crops) {
			if (lowest == null || cropPos.getY() < lowest.getY()) {
				lowest = cropPos;
			}
		}
		if (lowest == null) return;

		BlockState plantState;
		if (isKelp(cropBlock)) {
			if (!world.getBlockState(lowest).isOf(net.minecraft.block.Blocks.WATER)) return;
			if (!world.getBlockState(lowest.offset(Direction.DOWN)).isOf(net.minecraft.block.Blocks.WATER)) return;
			plantState = net.minecraft.block.Blocks.KELP.getDefaultState();
		} else {
			if (!world.getBlockState(lowest).isAir()) return;
			BlockPos below = lowest.offset(Direction.DOWN);
			BlockState belowState = world.getBlockState(below);
			if (!isValidCropBase(cropBlock, belowState)) return;
			plantState = cropBlock.getDefaultState();
		}

		if (player.getInventory().contains(item -> item.isOf(seed))) {
			if (world.setBlockState(lowest, plantState)) {
				player.getInventory().remove(item -> item.isOf(seed), 1, player.getInventory());
			}
		}
	}

	private static boolean isValidCropBase(Block crop, BlockState below) {
		if (crop instanceof CactusBlock) {
			return below.isOf(net.minecraft.block.Blocks.CACTUS)
					|| below.isIn(net.minecraft.registry.tag.BlockTags.SAND);
		}
		if (crop instanceof SugarCaneBlock) {
			return below.isIn(net.minecraft.registry.tag.BlockTags.DIRT)
					|| below.isIn(net.minecraft.registry.tag.BlockTags.SAND);
		}
		if (crop instanceof BambooBlock) {
			return below.isIn(net.minecraft.registry.tag.BlockTags.BAMBOO_PLANTABLE_ON)
					|| below.isOf(net.minecraft.block.Blocks.BAMBOO_SAPLING);
		}
		return true;
	}

	private static Set<BlockPos> findConnectedCrops(ServerWorld world, BlockPos start, Block startBlock) {
		Set<BlockPos> visited = new HashSet<>();
		visited.add(start);

		searchDirection(world, start, startBlock, Direction.UP, visited, MAX_DISTANCE);
		searchDirection(world, start, startBlock, Direction.DOWN, visited, MAX_DISTANCE);

		return visited;
	}

	private static void searchDirection(ServerWorld world, BlockPos start, Block targetBlock,
			Direction direction, Set<BlockPos> visited, int remaining) {
		if (remaining <= 0) return;

		BlockPos current = start;
		for (int i = 0; i < remaining; i++) {
			BlockPos next = current.offset(direction);
			if (visited.contains(next)) break;
			BlockState state = world.getBlockState(next);
			if (!sameCrop(targetBlock, state.getBlock())) break;
			visited.add(next);
			current = next;
		}
	}

	private static boolean isSupportedCrop(Block block) {
		return block instanceof SugarCaneBlock
				|| block instanceof BambooBlock
				|| block instanceof KelpBlock
				|| block instanceof KelpPlantBlock
				|| block instanceof CactusBlock;
	}

	private static boolean isKelp(Block block) {
		return block instanceof KelpBlock || block instanceof KelpPlantBlock;
	}

	private static boolean sameCrop(Block target, Block candidate) {
		if (isKelp(target) && isKelp(candidate)) return true;
		return target == candidate;
	}
}
