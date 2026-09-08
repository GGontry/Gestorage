package com.gontry.gestorage.mixin;

import com.gontry.gestorage.careful.CarefulBreakManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemFrameEntity.class)
public abstract class ItemFrameEntityMixin {
	@Shadow
	private boolean fixed;

	@Shadow
	private float itemDropChance;

	@Shadow
	private void removeFromFrame(ItemStack stack) {}

	@Shadow
	protected abstract ItemStack getAsItemStack();

	@Inject(method = "dropHeldStack", at = @At("HEAD"), cancellable = true)
	private void gestorage$dropHeldStack(Entity entity, boolean alwaysDrop, CallbackInfo ci) {
		if (!(entity instanceof ServerPlayerEntity player)) return;
		if (this.fixed) return;
		if (!CarefulBreakManager.shouldCollectEntityDrops(player)) return;

		ItemFrameEntity self = (ItemFrameEntity) (Object) this;
		ItemStack held = self.getHeldItemStack();
		self.setHeldItemStack(ItemStack.EMPTY);

		if (!self.getWorld().getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
			if (entity == null) this.removeFromFrame(held);
			ci.cancel();
			return;
		}
		if (player.isInCreativeMode()) {
			this.removeFromFrame(held);
			ci.cancel();
			return;
		}

		List<ItemStack> drops = new ArrayList<>();
		if (alwaysDrop) drops.add(this.getAsItemStack());
		if (!held.isEmpty()) {
			ItemStack copy = held.copy();
			this.removeFromFrame(copy);
			if (self.getRandom().nextFloat() < this.itemDropChance) drops.add(copy);
		}
		CarefulBreakManager.collectDrops(player, drops);
		ci.cancel();
	}
}