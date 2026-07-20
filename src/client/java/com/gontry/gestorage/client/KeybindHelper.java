package com.gontry.gestorage.client;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class KeybindHelper {
	public static final int MOD_CTRL = 1;
	public static final int MOD_SHIFT = 2;
	public static final int MOD_ALT = 4;
	public static final int MOD_SUPER = 8;

	private static final Map<String, Integer> keyCodeCache = new HashMap<>();

	public static String encode(int keyCode, int modifiers) {
		if (keyCode == -1) return "";
		StringBuilder sb = new StringBuilder();
		if ((modifiers & MOD_CTRL) != 0) sb.append("ctrl+");
		if ((modifiers & MOD_SHIFT) != 0) sb.append("shift+");
		if ((modifiers & MOD_ALT) != 0) sb.append("alt+");
		if ((modifiers & MOD_SUPER) != 0) sb.append("super+");
		if (keyCode < 0) {
			sb.append("mouse.").append(-keyCode);
		} else {
			String name = GLFW.glfwGetKeyName(keyCode, 0);
			if (name != null) {
				sb.append(name.toLowerCase());
			} else {
				sb.append("key_").append(keyCode);
			}
		}
		return sb.toString();
	}

	public static int[] decode(String s) {
		if (s == null || s.isEmpty()) return new int[]{-1, 0};
		int modifiers = 0;
		String keyPart = s;
		if (keyPart.startsWith("ctrl+")) { modifiers |= MOD_CTRL; keyPart = keyPart.substring(5); }
		if (keyPart.startsWith("shift+")) { modifiers |= MOD_SHIFT; keyPart = keyPart.substring(6); }
		if (keyPart.startsWith("alt+")) { modifiers |= MOD_ALT; keyPart = keyPart.substring(4); }
		if (keyPart.startsWith("super+")) { modifiers |= MOD_SUPER; keyPart = keyPart.substring(6); }
		int keyCode;
		if (keyPart.startsWith("mouse.")) {
			keyCode = -Integer.parseInt(keyPart.substring(6));
		} else if (keyPart.startsWith("key_")) {
			keyCode = Integer.parseInt(keyPart.substring(4));
		} else {
			keyCode = getKeyCodeByName(keyPart);
		}
		return new int[]{keyCode, modifiers};
	}

	private static int getKeyCodeByName(String name) {
		Integer cached = keyCodeCache.get(name.toLowerCase());
		if (cached != null) return cached;
		for (int i = 32; i <= 96; i++) {
			String n = GLFW.glfwGetKeyName(i, 0);
			if (n != null && n.equalsIgnoreCase(name)) {
				keyCodeCache.put(name.toLowerCase(), i);
				return i;
			}
		}
		for (int i = 256; i <= 348; i++) {
			String n = GLFW.glfwGetKeyName(i, 0);
			if (n != null && n.equalsIgnoreCase(name)) {
				keyCodeCache.put(name.toLowerCase(), i);
				return i;
			}
		}
		return -1;
	}

	public static boolean isPressed(String keybind, long windowHandle) {
		if (keybind == null || keybind.isEmpty()) return false;
		int[] decoded = decode(keybind);
		int keyCode = decoded[0];
		int requiredMods = decoded[1];
		if (keyCode == -1) return false;

		int ctrlState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL);
		int rCtrlState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL);
		int shiftState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT);
		int rShiftState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT);
		int altState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_ALT);
		int rAltState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_ALT);
		int superState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SUPER);
		int rSuperState = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SUPER);

		boolean ctrl = ctrlState == GLFW.GLFW_PRESS || rCtrlState == GLFW.GLFW_PRESS;
		boolean shift = shiftState == GLFW.GLFW_PRESS || rShiftState == GLFW.GLFW_PRESS;
		boolean alt = altState == GLFW.GLFW_PRESS || rAltState == GLFW.GLFW_PRESS;
		boolean super_ = superState == GLFW.GLFW_PRESS || rSuperState == GLFW.GLFW_PRESS;

		if ((requiredMods & MOD_CTRL) != 0 && !ctrl) return false;
		if ((requiredMods & MOD_SHIFT) != 0 && !shift) return false;
		if ((requiredMods & MOD_ALT) != 0 && !alt) return false;
		if ((requiredMods & MOD_SUPER) != 0 && !super_) return false;
		if ((requiredMods & MOD_CTRL) == 0 && ctrl) return false;
		if ((requiredMods & MOD_SHIFT) == 0 && shift) return false;
		if ((requiredMods & MOD_ALT) == 0 && alt) return false;
		if ((requiredMods & MOD_SUPER) == 0 && super_) return false;

		if (keyCode < 0) {
			int mouseButton = -keyCode;
			return GLFW.glfwGetMouseButton(windowHandle, mouseButton) == GLFW.GLFW_PRESS;
		} else {
			return GLFW.glfwGetKey(windowHandle, keyCode) == GLFW.GLFW_PRESS;
		}
	}

	public static String getKeyName(String keybind) {
		if (keybind == null || keybind.isEmpty()) return "NONE";
		String display = keybind.toUpperCase().replace("+", " + ");
		if (display.contains("MOUSE.")) {
			int mouseIdx = Integer.parseInt(display.substring(display.indexOf("MOUSE.") + 6));
			display = display.substring(0, display.indexOf("MOUSE.")) + "MB" + (mouseIdx + 1);
		}
		return display;
	}
}
