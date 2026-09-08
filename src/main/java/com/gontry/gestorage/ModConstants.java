package com.gontry.gestorage;

/**
 * Central registry for the numeric constants shared across Gestorage modules.
 *
 * Any magic number referenced from more than one place belongs here so a human
 * extending the mod can find and reason about it in a single location.
 */
public final class ModConstants {
	private ModConstants() {}

	/*
	 * Ender chest container capacities, one per storage size mode
	 * ({@link #MODE_NORMAL}, {@link #MODE_LARGE}, {@link #MODE_EXTRA_LARGE}).
	 */
	public static final int NORMAL_ENDER_SIZE = 27;
	public static final int LARGE_ENDER_SIZE = 54;
	public static final int EXTRA_LARGE_ENDER_SIZE = 228;
	public static final int SHULKER_BOX_SIZE = 27;

	/**
	 * Literal capacity of a vanilla ender chest. Kept as an alias of
	 * {@link #NORMAL_ENDER_SIZE} so call sites doing overflow math express that
	 * they mean the vanilla baseline rather than "whatever the normal mode is".
	 */
	public static final int VANILLA_ENDER_SIZE = NORMAL_ENDER_SIZE;

	/*
	 * Storage size mode identifiers. These values are persisted in the ender
	 * chest gamerule and shipped in sync packets, so they must never change
	 * once a release goes out.
	 */
	public static final int MODE_NORMAL = 0;
	public static final int MODE_LARGE = 1;
	public static final int MODE_EXTRA_LARGE = 2;

	/** Ticks the client waits before trusting its ender size sync data. */
	public static final int MAX_SYNC_DELAY_TICKS = 5;

	/** Maps a persisted size mode to the matching container capacity. */
	public static int getEnderSizeForMode(int mode) {
		return switch (mode) {
			case MODE_LARGE -> LARGE_ENDER_SIZE;
			case MODE_EXTRA_LARGE -> EXTRA_LARGE_ENDER_SIZE;
			default -> NORMAL_ENDER_SIZE;
		};
	}
}