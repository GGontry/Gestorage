package com.gontry.gestorage.mixin;

import com.gontry.gestorage.careful.CarefulBreakManager;
import net.minecraft.entity.Entity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {
	@Inject(method = "syncWorldEvent", at = @At("HEAD"), cancellable = true)
	private void gestorage$suppressBlockBreakEffect(Entity player, int eventId, BlockPos pos, int data, CallbackInfo ci) {
		if (CarefulBreakManager.suppressBlockEffects && eventId == 2001) {
			ci.cancel();
		}
	}

	@Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
	private void gestorage$suppressBlockSound(Entity except, double x, double y, double z,
			RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed, CallbackInfo ci) {
		if (CarefulBreakManager.suppressBlockEffects && category == SoundCategory.BLOCKS) {
			ci.cancel();
		}
	}
}
