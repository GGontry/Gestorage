package com.gontry.gestorage.network;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.ModConstants;
import com.gontry.gestorage.inventory.EnderChestFactory;
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
			if (sourceInv == null) {
				Gestorage.LOGGER.warn("[Refill] Source inventory not found: type={}, player={}", sourceType, player.getUuid());
				return;
			}

			Inventory targetInv = sameInv ? sourceInv : getSourceInventory(player, targetType);
			if (targetInv == null) {
				Gestorage.LOGGER.warn("[Refill] Target inventory not found: type={}, player={}", targetType, player.getUuid());
				return;
			}

			if (payload.sourceSlot() < 0 || payload.sourceSlot() >= sourceInv.size()) {
				Gestorage.LOGGER.warn("[Refill] Invalid source slot {} (size={})", payload.sourceSlot(), sourceInv.size());
				return;
			}

			ItemStack sourceStack = sourceInv.getStack(payload.sourceSlot());
			if (sourceStack.isEmpty() || !ShulkerRefillManager.isShulkerBox(sourceStack)) return;

			ContainerComponent container = sourceStack.get(DataComponentTypes.CONTAINER);
			if (container == null) return;

			DefaultedList<ItemStack> shulkerContents = DefaultedList.ofSize(ModConstants.SHULKER_BOX_SIZE, ItemStack.EMPTY);
			container.copyTo(shulkerContents);

			if (payload.targetSlot() < 0 || payload.targetSlot() >= targetInv.size()) {
				Gestorage.LOGGER.warn("[Refill] Invalid target slot {} (size={})", payload.targetSlot(), targetInv.size());
				return;
			}

			ItemStack targetStack = targetInv.getStack(payload.targetSlot());

			ItemStack targetTypeStack = null;
			if (targetStack.isEmpty()) {
				for (ItemStack stack : shulkerContents) {
					if (!stack.isEmpty()) {
						targetTypeStack = stack.copy();
						targetTypeStack.setCount(1);
						break;
					}
				}
			} else {
				for (ItemStack stack : shulkerContents) {
					if (!stack.isEmpty()
							&& targetStack.isOf(stack.getItem())
							&& targetStack.getMaxCount() == stack.getMaxCount()) {
						targetTypeStack = stack.copy();
						targetTypeStack.setCount(1);
						break;
					}
				}
			}
			if (targetTypeStack == null) return;

			int available = 0;
			for (ItemStack stack : shulkerContents) {
				if (!stack.isEmpty() && ItemStack.areItemsAndComponentsEqual(targetTypeStack, stack)) {
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
				if (!stack.isEmpty() && ItemStack.areItemsAndComponentsEqual(targetTypeStack, stack)) {
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

			Gestorage.LOGGER.debug("[Refill] Moved {}x {} from {}:{} to {}:{}", toGive, targetTypeStack.getItem(), sourceType, payload.sourceSlot(), targetType, payload.targetSlot());
		});
	}

	private static Inventory getSourceInventory(ServerPlayerEntity player, String type) {
		return switch (type) {
			case "ender_normal" -> player.getEnderChestInventory();
			case "ender_large" -> EnderChestFactory.createForPlayer(player, ModConstants.LARGE_ENDER_SIZE);
			case "ender_xlarge" -> EnderChestFactory.createForPlayer(player, ModConstants.EXTRA_LARGE_ENDER_SIZE);
			default -> player.getInventory();
		};
	}
}
