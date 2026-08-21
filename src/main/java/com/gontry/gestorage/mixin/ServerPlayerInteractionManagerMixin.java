package com.gontry.gestorage.mixin;

import com.gontry.gestorage.careful.BetterHarvesting;
import com.gontry.gestorage.careful.CarefulBreakManager;
import com.gontry.gestorage.careful.TreeCapitator;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
	@Shadow
	public ServerPlayerEntity player;
	@Shadow
	public ServerWorld world;

	@Unique
	private static final ThreadLocal<Boolean> GESTORAGE_PROCESSING = ThreadLocal.withInitial(() -> false);
	@Unique
	private static final ThreadLocal<BlockState> GESTORAGE_CACHED_STATE = new ThreadLocal<>();

	@Inject(method = "tryBreakBlock", at = @At("HEAD"))
	private void gestorage$tryBreakBlockHead(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (world.isClient()) return;
		BlockState state = world.getBlockState(pos);
		GESTORAGE_CACHED_STATE.set(state);
	}

	@Inject(method = "tryBreakBlock", at = @At("RETURN"))
	private void gestorage$tryBreakBlockReturn(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		try {
			if (!cir.getReturnValue()) return;
			if (GESTORAGE_PROCESSING.get()) return;
			if (world.isClient()) return;

			BlockState state = GESTORAGE_CACHED_STATE.get();
			if (state == null || state.isAir()) return;

			if (CarefulBreakManager.isTreeCapitatorActive(player)) {
				GESTORAGE_PROCESSING.set(true);
				try {
					TreeCapitator.breakTree(player, world, pos, state);
				} finally {
					GESTORAGE_PROCESSING.remove();
				}
			}
			if (CarefulBreakManager.isBetterHarvestingActive(player)) {
				GESTORAGE_PROCESSING.set(true);
				try {
					BetterHarvesting.breakCrops(player, world, pos, state);
				} finally {
					GESTORAGE_PROCESSING.remove();
				}
			}
		} finally {
			GESTORAGE_CACHED_STATE.remove();
		}
	}
}
