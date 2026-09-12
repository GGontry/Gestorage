package com.gontry.gestorage.mixin;

import com.gontry.gestorage.toolwheel.AutoToolManager;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerInteractionManager.class)
public class ToolWheelMiningMixin {
	@Shadow protected ServerWorld world;
	@Shadow @Final protected ServerPlayerEntity player;
	@Shadow private boolean mining;
	@Shadow private BlockPos miningPos;

	@Inject(method = "processBlockBreakingAction", at = @At("HEAD"))
	private void gestorage$onProcessBlockBreakingAction(BlockPos pos, PlayerActionC2SPacket.Action action,
			Direction direction, int worldHeight, int sequence, CallbackInfo ci) {
		if (action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
			AutoToolManager.onMineStart(player, world, pos);
		}
	}

	@Inject(method = "update", at = @At("HEAD"))
	private void gestorage$onMiningUpdate(CallbackInfo ci) {
		if (mining && miningPos != null && !world.getBlockState(miningPos).isAir()) {
			AutoToolManager.refresh(player);
		}
	}
}