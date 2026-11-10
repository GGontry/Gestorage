package com.gontry.gestorage.shulker;

/**
 * Marks the thread section where a hopper is moving items.
 *
 * <p>{@code ItemStack.getMaxCount()} has no world or inventory context, so it
 * cannot know whether a shulker is being queried for a player or for a hopper.
 * The hopper mixins set this flag around the tick that performs the transfer,
 * which lets the stack size mixin keep vanilla behaviour (max 1) for hoppers
 * while players still get the stackable size.
 *
 * <p>The flag is a depth counter rather than a boolean: a hopper tick can call
 * another helper that re-enters the context (for example a modded hopper
 * implementation), and an inner exit must not clear the flag for the outer
 * caller. The hopper mixins always exit in a {@code finally} block so an
 * exception cannot leave the thread marked as a hopper.
 */
public final class ShulkerStackContext {
	private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<>();

	private ShulkerStackContext() {}

	public static void enter() {
		DEPTH.set(depth() + 1);
	}

	public static void exit() {
		int depth = depth() - 1;
		if (depth <= 0) {
			DEPTH.remove();
		} else {
			DEPTH.set(depth);
		}
	}

	public static boolean active() {
		return depth() > 0;
	}

	private static int depth() {
		Integer depth = DEPTH.get();
		return depth == null ? 0 : depth;
	}
}
