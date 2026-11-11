package com.gontry.gestorage.mixin;

import com.gontry.gestorage.GameRuleNameMigration;
import com.mojang.serialization.Dynamic;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.world.level.LevelInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures the pre-1.21.11 game rule values from the raw level data before
 * {@link LevelInfo} parses (and drops) them, so {@link GameRuleNameMigration} can
 * import them into the renamed rules once the server is up.
 */
@Mixin(LevelInfo.class)
public class LevelInfoMixin {
	@Inject(method = "fromDynamic", at = @At("HEAD"))
	private static void gestorage$captureLegacyRuleNames(Dynamic<?> dynamic, DataConfiguration dataConfiguration,
			CallbackInfoReturnable<LevelInfo> cir) {
		GameRuleNameMigration.captureFromLevel(dynamic);
	}
}
