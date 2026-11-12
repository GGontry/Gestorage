package com.gontry.gestorage.client;

import com.gontry.gestorage.client.config.ModuleConfig;
import com.gontry.gestorage.network.ModNetworking;
import com.gontry.gestorage.shulker.ShulkerStackRule;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public class ModNetworkingClient {
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_ENDER_SCREEN, OpenEnderScreenS2CPacket::handle);
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.ENDER_SIZE_CHANGED, EnderSizeChangedS2CPacket::handle);
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.CAREFUL_BREAK_STATE, CarefulBreakStateS2CPacket::handle);
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SHULKER_STACK_STATE, ShulkerStackStateS2CPacket::handle);
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.TOOL_WHEEL_SYNC, ToolWheelSyncS2CPacket::handle);
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.TOOL_WHEEL_ENCH_PREF_STATE,
				ToolWheelEnchPrefS2CPacket::handle);
		ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SLOT_LOCK_SYNC, SlotLockSyncS2CPacket::handle);

		// Mirrored server state belongs to a world: drop it when leaving one.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ShulkerStackRule.resetClientMirror();
			ClientSlotLockState.clear();
		});
	}

	/**
	 * The config screen is also the Mod Menu entry point, so it can be open before
	 * a world is joined. Sending a packet without a connection throws, and every
	 * server-owned option must be a harmless no-op in that state.
	 */
	public static boolean isInWorld() {
		return MinecraftClient.getInstance().getNetworkHandler() != null;
	}

	public static void sendOpenEnderChest() {
		if (!isInWorld()) return;
		ClientPlayNetworking.send(new ModNetworking.OpenEnderChestC2S());
	}

	public static void sendSortInventory(boolean mergeStacks, boolean sortByName, boolean sortDescending) {
		if (!isInWorld()) return;
		ClientPlayNetworking.send(new ModNetworking.SortInventoryC2S(mergeStacks, sortByName, sortDescending));
	}

	public static void sendToggleCarefulBreak(int option) {
		if (!isInWorld()) return;
		ClientPlayNetworking.send(new ModNetworking.ToggleCarefulBreakC2S(option));
	}

	public static void sendOpenToolWheel() {
		if (!isInWorld()) return;
		ClientPlayNetworking.send(new ModNetworking.ToolWheelOpenC2S());
	}

	public static void sendToolWheelSwap(int slot) {
		if (!isInWorld()) return;
		ClientPlayNetworking.send(new ModNetworking.ToolWheelSwapC2S(slot));
	}

	public static void sendToggleAutoTool() {
		if (!isInWorld()) return;
		ClientPlayNetworking.send(new ModNetworking.ToolWheelAutoC2S());
	}

	public static void sendModuleEnabled(boolean enabled) {
		if (!isInWorld()) return;
		ClientPlayNetworking.send(new ModNetworking.ToolWheelModuleC2S(enabled));
	}

	public static void sendEnchPref(int pref) {
		if (!isInWorld()) return;
		ClientPlayNetworking.send(new ModNetworking.ToolWheelEnchPrefC2S(pref));
	}

	public static void sendSlotLockToggle(int scope, int slot) {
		if (!isInWorld()) return;
		ClientPlayNetworking.send(new ModNetworking.SlotLockToggleC2S(scope, slot));
	}

	public static void sendSlotLockConfig() {
		// The JOIN handler sends the settings again once a world is joined.
		if (!isInWorld()) return;
		ClientPlayNetworking.send(new ModNetworking.SlotLockConfigC2S(
				ModuleConfig.slotLock().enabled(),
				ModuleConfig.slotLock().sortingSkipsLockedSlots(),
				ModuleConfig.slotLock().preventDrop(),
				ModuleConfig.slotLock().preventUse()
		));
	}
}
