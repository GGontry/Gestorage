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
	@Shadow private boolean failedToMine;
	@Shadow private BlockPos failedMiningPos;

	/**
	 * Runs before vanilla validates the action, so the position is checked here as
	 * well: {@link ServerPlayerEntity#canInteractWithBlockAt} keeps the lookup inside
	 * the chunks the player already has loaded instead of letting a bogus position
	 * force a chunk load.
	 */
	@Inject(method = "processBlockBreakingAction", at = @At("HEAD"))
	private void gestorage$onProcessBlockBreakingAction(BlockPos pos, PlayerActionC2SPacket.Action action,
			Direction direction, int worldHeight, int sequence, CallbackInfo ci) {
		if (action != PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) return;
		if (pos.getY() < world.getBottomY() || pos.getY() >= worldHeight) return;
		if (!player.canInteractWithBlockAt(pos, 1.0D)) return;
		AutoToolManager.onMineStart(player, world, pos);
	}

	/**
	 * Refreshes the mining timer and re-evaluates the tool while a block is really
	 * being broken. Both vanilla mining states are covered: the normal one and the
	 * "failed to mine" fallback vanilla enters when the client already considers the
	 * block broken. Ignoring the fallback let the pending revert land in the middle of
	 * a vein.
	 */
	@Inject(method = "update", at = @At("HEAD"))
	private void gestorage$onMiningUpdate(CallbackInfo ci) {
		BlockPos target;
		if (failedToMine) {
			target = failedMiningPos;
		} else if (mining) {
			target = miningPos;
		} else {
			return;
		}
		if (target == null) return;
		if (world.getBlockState(target).isAir()) return;
		AutoToolManager.refresh(player);
		if (player.age % AutoToolManager.AUTO_EVAL_INTERVAL_TICKS == 0) {
			AutoToolManager.onMineStart(player, world, target);
		}
	}
}
