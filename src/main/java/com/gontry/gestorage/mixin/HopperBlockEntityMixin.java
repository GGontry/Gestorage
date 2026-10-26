package com.gontry.gestorage.mixin;

import com.gontry.gestorage.shulker.ShulkerStackContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks the whole hopper tick as a hopper item-transfer context, so the shulker
 * stack-size mixin can keep empty shulkers at max 1 for hoppers while still
 * allowing players to stack them.
 */
@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {
	@Inject(method = "serverTick", at = @At("HEAD"))
	private static void gestorage$enterHopperTick(World world, BlockPos pos, BlockState state,
			HopperBlockEntity blockEntity, CallbackInfo ci) {
		ShulkerStackContext.enter();
	}

	@Inject(method = "serverTick", at = @At("RETURN"))
	private static void gestorage$exitHopperTick(World world, BlockPos pos, BlockState state,
			HopperBlockEntity blockEntity, CallbackInfo ci) {
		ShulkerStackContext.exit();
	}
}
