package com.gontry.gestorage.mixin;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.careful.CarefulBreakManager;
import com.gontry.gestorage.config.CarefulBreakServerConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class EntityDropMixin {
	@Unique
	private static final ThreadLocal<ServerPlayerEntity> GESTORAGE_CURRENT_KILLER = new ThreadLocal<>();

	@Inject(method = "onDeath", at = @At("HEAD"))
	private void gestorage$onDeath(DamageSource source, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.getWorld().isClient()) return;
		if (!CarefulBreakServerConfig.enabled) return;
		if (!CarefulBreakServerConfig.carefulDrop && !CarefulBreakServerConfig.alwaysCareful) return;

		Entity attacker = source.getAttacker();
		if (attacker instanceof ServerPlayerEntity player) {
			GESTORAGE_CURRENT_KILLER.set(player);
		}
	}

	@Inject(method = "dropLoot", at = @At("HEAD"), cancellable = true)
	private void gestorage$dropLoot(DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
		ServerPlayerEntity killer = GESTORAGE_CURRENT_KILLER.get();
		if (killer == null) return;
		if (!CarefulBreakServerConfig.alwaysCareful && !killer.isSneaking()) return;

		LivingEntity self = (LivingEntity) (Object) this;
		ServerWorld world = (ServerWorld) self.getWorld();

		try {
			LootTable lootTable = world.getServer().getReloadableRegistries().getLootTable(self.getLootTable());
			Vec3d pos = self.getPos();
			LootContextParameterSet.Builder builder = new LootContextParameterSet.Builder(world)
					.add(LootContextParameters.THIS_ENTITY, self)
					.add(LootContextParameters.ORIGIN, pos)
					.add(LootContextParameters.DAMAGE_SOURCE, source)
					.addOptional(LootContextParameters.ATTACKING_ENTITY, killer)
					.addOptional(LootContextParameters.DIRECT_ATTACKING_ENTITY, source.getSource());
			if (causedByPlayer) {
				builder.add(LootContextParameters.LAST_DAMAGE_PLAYER, killer);
				builder.luck(killer.getLuck());
			}
			LootContextParameterSet params = builder.build(LootContextTypes.ENTITY);

			java.util.List<ItemStack> drops = lootTable.generateLoot(params, self.getLootTableSeed());
			CarefulBreakManager.collectDrops(killer, drops);
			ci.cancel();
		} catch (Exception e) {
			Gestorage.LOGGER.error("[CarefulDrop] error generating loot", e);
		}
	}

	@Inject(method = "onDeath", at = @At("RETURN"))
	private void gestorage$onDeathReturn(DamageSource source, CallbackInfo ci) {
		GESTORAGE_CURRENT_KILLER.remove();
	}
}
