package com.gontry.gestorage.client;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * String-based keybind DSL shared by every module (e.g. "ctrl+shift+e", "mouse.2",
 * "left_alt"). A modifier can be the whole binding, not just a prefix: those keys
 * use stable side-specific names so a hand-edited config stays readable.
 *
 * Keybinds are plain config strings rather than vanilla KeyBinding objects so
 * they survive config round-trips unchanged. Invariants:
 * - encode()/decode() are each other's inverse for keys written by the config UI.
 * - decode() and getKeyName() must never throw: config files can be hand-edited.
 * - Configured modifiers are required, but unrelated ones are ignored, so a
 *   binding still works while sneaking (Shift) or holding any other modifier.
 */
public class KeybindHelper {
	public static final int MOD_CTRL = 1;
	public static final int MOD_SHIFT = 2;
	public static final int MOD_ALT = 4;
	public static final int MOD_SUPER = 8;

	/** Stable names for modifier keys, which must stay bindable on their own. */
	private static final Map<Integer, String> MODIFIER_NAMES = Map.of(
			GLFW.GLFW_KEY_LEFT_CONTROL, "left_ctrl",
			GLFW.GLFW_KEY_RIGHT_CONTROL, "right_ctrl",
			GLFW.GLFW_KEY_LEFT_SHIFT, "left_shift",
			GLFW.GLFW_KEY_RIGHT_SHIFT, "right_shift",
			GLFW.GLFW_KEY_LEFT_ALT, "left_alt",
			GLFW.GLFW_KEY_RIGHT_ALT, "right_alt",
			GLFW.GLFW_KEY_LEFT_SUPER, "left_super",
			GLFW.GLFW_KEY_RIGHT_SUPER, "right_super"
	);

	private static final Map<String, Integer> MODIFIER_CODES = Map.of(
			"left_ctrl", GLFW.GLFW_KEY_LEFT_CONTROL,
			"right_ctrl", GLFW.GLFW_KEY_RIGHT_CONTROL,
			"left_shift", GLFW.GLFW_KEY_LEFT_SHIFT,
			"right_shift", GLFW.GLFW_KEY_RIGHT_SHIFT,
			"left_alt", GLFW.GLFW_KEY_LEFT_ALT,
			"right_alt", GLFW.GLFW_KEY_RIGHT_ALT,
			"left_super", GLFW.GLFW_KEY_LEFT_SUPER,
			"right_super", GLFW.GLFW_KEY_RIGHT_SUPER
	);

	private static final Map<String, Integer> keyCodeCache = new HashMap<>();

	/**
	 * Encodes a key and its modifiers. When the key itself is a modifier its own
	 * family is removed from the prefix bits, so Left Alt alone becomes
	 * {@code "left_alt"} instead of the redundant {@code "alt+left_alt"}.
	 */
	public static String encode(int keyCode, int modifiers) {
		if (keyCode == -1) return "";
		int prefixMods = modifiers & ~modifierFamily(keyCode);
		StringBuilder sb = new StringBuilder();
		if ((prefixMods & MOD_CTRL) != 0) sb.append("ctrl+");
		if ((prefixMods & MOD_SHIFT) != 0) sb.append("shift+");
		if ((prefixMods & MOD_ALT) != 0) sb.append("alt+");
		if ((prefixMods & MOD_SUPER) != 0) sb.append("super+");
		if (keyCode < 0) {
			sb.append("mouse.").append(-keyCode);
		} else {
			String modifierName = MODIFIER_NAMES.get(keyCode);
			if (modifierName != null) {
				sb.append(modifierName);
			} else {
				String name = GLFW.glfwGetKeyName(keyCode, 0);
				if (name != null) {
					sb.append(name.toLowerCase());
				} else {
					sb.append("key_").append(keyCode);
				}
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
		Integer modifierCode = modifierCode(keyPart);
		if (modifierCode != null) {
			keyCode = modifierCode;
		} else if (keyPart.startsWith("mouse.")) {
			keyCode = parseMouseKey(keyPart.substring(6));
		} else if (keyPart.startsWith("key_")) {
			keyCode = parseKeyNumber(keyPart.substring(4));
		} else {
			keyCode = getKeyCodeByName(keyPart);
		}
		return new int[]{keyCode, modifiers};
	}

	/** Accepts both the canonical {@code left_alt} and the GLFW-style {@code Left Alt}. */
	private static Integer modifierCode(String name) {
		return MODIFIER_CODES.get(name.trim().toLowerCase().replace(' ', '_'));
	}

	/** Modifier family a key belongs to, or 0 for ordinary keys and mouse buttons. */
	private static int modifierFamily(int keyCode) {
		return switch (keyCode) {
			case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> MOD_CTRL;
			case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> MOD_SHIFT;
			case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> MOD_ALT;
			case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> MOD_SUPER;
			default -> 0;
		};
	}

	/**
	 * Parses a numeric key fragment, mapping any corrupt value to an "unbound"
	 * key (-1) instead of throwing. A hand-edited config must never crash the
	 * tick handler that polls keybinds.
	 */
	private static int parseKeyNumber(String fragment) {
		try {
			return Integer.parseInt(fragment);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/** Parses "mouse.N" fragments to the negative key code used internally. */
	private static int parseMouseKey(String fragment) {
		int n = parseKeyNumber(fragment);
		return n < 0 ? -1 : -n;
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

		// Only the configured modifiers are required. Extra ones are ignored so
		// bindings keep working while sneaking or while another module's modifier
		// is held; a modifier bound as the key is covered by its own GLFW state.
		if ((requiredMods & MOD_CTRL) != 0 && !ctrl) return false;
		if ((requiredMods & MOD_SHIFT) != 0 && !shift) return false;
		if ((requiredMods & MOD_ALT) != 0 && !alt) return false;
		if ((requiredMods & MOD_SUPER) != 0 && !super_) return false;

		if (keyCode < 0) {
			int mouseButton = -keyCode - 1;
			return GLFW.glfwGetMouseButton(windowHandle, mouseButton) == GLFW.GLFW_PRESS;
		} else {
			return GLFW.glfwGetKey(windowHandle, keyCode) == GLFW.GLFW_PRESS;
		}
	}

	public static String getKeyName(String keybind) {
		if (keybind == null || keybind.isEmpty()) return "NONE";
		String display = keybind.toUpperCase().replace('_', ' ').replace("+", " + ");
		if (display.contains("MOUSE.")) {
			try {
				String prefix = display.substring(0, display.indexOf("MOUSE."));
				int mouseIdx = Integer.parseInt(display.substring(display.indexOf("MOUSE.") + 6));
				return prefix + "MB" + mouseIdx;
			} catch (NumberFormatException e) {
				return display;
			}
		}
		return display;
	}
}
