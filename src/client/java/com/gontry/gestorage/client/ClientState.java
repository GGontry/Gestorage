package com.gontry.gestorage.client;

import com.gontry.gestorage.ModConstants;

/**
 * Client-side mirror of the server's ender chest size mode.
 *
 * Updated by {@code EnderSizeChangedS2CPacket} (the sender side is reserved).
 * It is the agreed hand-off point for client features that need to know the
 * ender chest capacity before a menu is opened.
 */
public final class ClientState {
	private ClientState() {}

	private static int cachedSizeMode = ModConstants.MODE_NORMAL;

	public static int getCachedSizeMode() {
		return cachedSizeMode;
	}

	public static void setCachedSizeMode(int mode) {
		cachedSizeMode = mode;
	}
}