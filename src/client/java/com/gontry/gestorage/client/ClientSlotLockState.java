package com.gontry.gestorage.client;

import java.util.HashSet;
import java.util.Set;

/**
 * Client mirror of the server's lock sets. Replaced wholesale by the sync packet
 * and cleared when leaving a world, so it can never leak locks across servers.
 */
public final class ClientSlotLockState {
	private static final Set<Integer> PLAYER_SLOTS = new HashSet<>();
	private static final Set<Integer> ENDER_SLOTS = new HashSet<>();

	private ClientSlotLockState() {}

	public static void apply(int[] playerSlots, int[] enderSlots) {
		PLAYER_SLOTS.clear();
		ENDER_SLOTS.clear();
		for (int slot : playerSlots) {
			PLAYER_SLOTS.add(slot);
		}
		for (int slot : enderSlots) {
			ENDER_SLOTS.add(slot);
		}
	}

	public static boolean isPlayerLocked(int slot) {
		return PLAYER_SLOTS.contains(slot);
	}

	public static boolean isEnderLocked(int slot) {
		return ENDER_SLOTS.contains(slot);
	}

	public static void clear() {
		PLAYER_SLOTS.clear();
		ENDER_SLOTS.clear();
	}
}
