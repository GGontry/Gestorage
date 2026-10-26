package com.gontry.gestorage.shulker;

/**
 * Marks the thread section where a hopper is moving items.
 *
 * <p>{@code ItemStack.getMaxCount()} has no world or inventory context, so it
 * cannot know whether a shulker is being queried for a player or for a hopper.
 * The hopper mixins set this flag around the tick that performs the transfer,
 * which lets the stack size mixin keep vanilla behaviour (max 1) for hoppers
 * while players still get the stackable size.
 */
public final class ShulkerStackContext {
	private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

	private ShulkerStackContext() {}

	public static void enter() {
		ACTIVE.set(true);
	}

	public static void exit() {
		ACTIVE.remove();
	}

	public static boolean active() {
		return ACTIVE.get();
	}
}
