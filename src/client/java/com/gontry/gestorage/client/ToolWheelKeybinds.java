package com.gontry.gestorage.client;

import com.gontry.gestorage.client.config.ModuleConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class ToolWheelKeybinds {
	private static boolean prevToggleEnabled = false;
	private static boolean prevOpenWheel = false;
	private static boolean prevAutoTool = false;
	private static boolean prevEnchPref = false;
	private static boolean prevWheelKey = false;

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			long handle = client.getWindow() != null ? client.getWindow().getHandle() : 0;
			boolean inGame = client.player != null && handle != 0 && client.currentScreen == null;
			boolean enabled = ModuleConfig.toolWheel().enabled();

			String toggleKey = ModuleConfig.toolWheel().toggleEnabledKey();
			boolean togglePressed = inGame && !toggleKey.isEmpty() && KeybindHelper.isPressed(toggleKey, handle);
			if (togglePressed && !prevToggleEnabled) {
				boolean newState = !ModuleConfig.toolWheel().enabled();
				ModuleConfig.toolWheel().enabled(newState);
				ModuleConfig.toolWheel().save();
				ModNetworkingClient.sendModuleEnabled(newState);
			}
			prevToggleEnabled = togglePressed;

			String openKey = ModuleConfig.toolWheel().openWheelKey();
			boolean openPressed = inGame && enabled && !openKey.isEmpty() && KeybindHelper.isPressed(openKey, handle);
			if (openPressed && !prevOpenWheel) {
				ModNetworkingClient.sendOpenToolWheel();
			}
			prevOpenWheel = openPressed;

			String autoKey = ModuleConfig.toolWheel().autoToolKey();
			boolean autoPressed = inGame && enabled && !autoKey.isEmpty() && KeybindHelper.isPressed(autoKey, handle);
			if (autoPressed && !prevAutoTool) {
				ModNetworkingClient.sendToggleAutoTool();
			}
			prevAutoTool = autoPressed;

			String enchPrefKey = ModuleConfig.toolWheel().enchPrefKey();
			boolean enchPrefPressed = inGame && enabled && !enchPrefKey.isEmpty()
					&& KeybindHelper.isPressed(enchPrefKey, handle);
			if (enchPrefPressed && !prevEnchPref) {
				ModNetworkingClient.sendEnchPref(ClientToolWheelState.nextEnchPref());
			}
			prevEnchPref = enchPrefPressed;

			String wheelKey = ModuleConfig.toolWheel().wheelKey();
			boolean wheelKeyPressed = inGame && enabled && !wheelKey.isEmpty() && KeybindHelper.isPressed(wheelKey, handle);
			if (wheelKeyPressed && !prevWheelKey) {
				if (ClientToolWheelState.wheelActive) {
					deactivate(client);
				} else {
					ClientToolWheelState.wheelActive = true;
					client.mouse.unlockCursor();
				}
			}
			prevWheelKey = wheelKeyPressed;

			if (ClientToolWheelState.wheelActive && (!inGame || !enabled)) {
				deactivate(client);
			}

			ClientToolWheelState.wheelClickSuppress = false;
		});
	}

	public static void select(MinecraftClient client) {
		if (!ClientToolWheelState.wheelActive) return;
		int slot = ToolWheelRenderer.computeHoverSlot(client);
		ClientToolWheelState.wheelActive = false;
		client.mouse.lockCursor();
		if (slot >= 0) {
			ModNetworkingClient.sendToolWheelSwap(slot);
		}
	}

	public static void deactivate(MinecraftClient client) {
		if (!ClientToolWheelState.wheelActive) return;
		ClientToolWheelState.wheelActive = false;
		client.mouse.lockCursor();
	}
}