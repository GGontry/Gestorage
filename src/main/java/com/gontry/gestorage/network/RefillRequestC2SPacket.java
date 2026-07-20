package com.gontry.gestorage.network;

import com.gontry.gestorage.inventory.EnhancedEnderChestInventory;
import com.gontry.gestorage.refill.ShulkerRefillManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

public class RefillRequestC2SPacket {
	public static void handle(ModNetworking.RefillRequestC2S payload, ServerPlayNetworking.Context ctx) {
		ctx.server().execute(() -> {
			ServerPlayerEntity player = ctx.player();
			if (player == null) return;

			String sourceType = payload.sourceType();
			String targetType = payload.targetType();
			boolean sameInv = sourceType.equals(targetType);

			Inventory sourceInv = getSourceInventory(player, sourceType);
			if (sourceInv == null) return;

			Inventory targetInv = sameInv ? sourceInv : getSourceInventory(player, targetType);
			if (targetInv == null) return;

			if (payload.sourceSlot() < 0 || payload.sourceSlot() >= sourceInv.size()) return;

			ItemStack sourceStack = sourceInv.getStack(payload.sourceSlot());
			if (sourceStack.isEmpty() || !ShulkerRefillManager.isShulkerBox(sourceStack)) return;

			ContainerComponent container = sourceStack.get(DataComponentTypes.CONTAINER);
			if (container == null) return;

			DefaultedList<ItemStack> shulkerContents = DefaultedList.ofSize(27, ItemStack.EMPTY);
			container.copyTo(shulkerContents);

			ItemStack targetTypeStack = null;
			for (ItemStack stack : shulkerContents) {
				if (!stack.isEmpty()) {
					targetTypeStack = stack.copy();
					targetTypeStack.setCount(1);
					break;
				}
			}
			if (targetTypeStack == null) return;

			if (payload.targetSlot() < 0 || payload.targetSlot() >= targetInv.size()) return;

			ItemStack targetStack = targetInv.getStack(payload.targetSlot());

			if (!targetStack.isEmpty() && targetStack.getItem() != targetTypeStack.getItem()) return;

			int available = 0;
			for (ItemStack stack : shulkerContents) {
				if (!stack.isEmpty() && stack.getItem() == targetTypeStack.getItem()) {
					available += stack.getCount();
				}
			}
			if (available <= 0) return;

			int maxStack = targetTypeStack.getMaxCount();
			int currentCount = targetStack.isEmpty() ? 0 : targetStack.getCount();
			int space = maxStack - currentCount;
			if (space <= 0) return;

			int toGive = Math.min(space, available);

			int remaining = toGive;
			for (int i = 0; i < shulkerContents.size() && remaining > 0; i++) {
				ItemStack stack = shulkerContents.get(i);
				if (!stack.isEmpty() && stack.getItem() == targetTypeStack.getItem()) {
					int toRemove = Math.min(remaining, stack.getCount());
					stack.decrement(toRemove);
					remaining -= toRemove;
				}
			}

			if (targetStack.isEmpty()) {
				targetInv.setStack(payload.targetSlot(), targetTypeStack.copyWithCount(toGive));
			} else {
				targetStack.setCount(currentCount + toGive);
			}

			sourceStack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(shulkerContents));
			sourceInv.markDirty();
			if (!sameInv) {
				targetInv.markDirty();
			}
		});
	}

	private static Inventory getSourceInventory(ServerPlayerEntity player, String type) {
		return switch (type) {
			case "ender_normal" -> player.getEnderChestInventory();
			case "ender_large" -> getEnhancedInventory(player, 54);
			case "ender_xlarge" -> getEnhancedInventory(player, 228);
			default -> player.getInventory();
		};
	}

	private static EnhancedEnderChestInventory getEnhancedInventory(ServerPlayerEntity player, int size) {
		var stateManager = player.getWorld().getServer().getOverworld().getPersistentStateManager();
		return new EnhancedEnderChestInventory(
			player.getEnderChestInventory(), size, stateManager, player.getUuid()
		);
	}
}
