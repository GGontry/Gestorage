package com.gontry.gestorage.shulker;

import com.gontry.gestorage.ModGameRules;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;

/**
 * Resolves the {@code gestorage:stackableShulkers} game rule for call sites that have no
 * world at hand, such as {@code ItemStack.getMaxCount()}.
 *
 * <p>The server holds a live reference to its {@link GameRules} instance. Rule objects
 * mutate in place, so the reference always returns the current value, including after a
 * vanilla {@code /gamerule} change, with no cache to invalidate.
 *
 * <p>A client has no server instance and Minecraft 1.21.1 does not sync game rules to it,
 * so the client reads the mirror the server pushes with the {@code shulker_stack_state}
 * packet instead.
 */
public final class ShulkerStackRule {
	private static volatile GameRules serverRules;
	private static volatile boolean clientMirror;

	private ShulkerStackRule() {}

	public static void onServerStarted(MinecraftServer server) {
		serverRules = server.getGameRules();
	}

	public static void onServerStopped() {
		serverRules = null;
	}

	public static boolean enabled() {
		GameRules rules = serverRules;
		if (rules != null) {
			return ModGameRules.isStackableShulkers(rules);
		}
		return clientMirror;
	}

	/**
	 * Whether hoppers may treat empty shulkers as stackable. Always true on the
	 * client: the hopper context only runs on the logical server, and a client-side
	 * render tick must keep seeing the vanilla stack size.
	 */
	public static boolean hoppersAllowed() {
		GameRules rules = serverRules;
		if (rules != null) {
			return ModGameRules.isHopperStackableShulkers(rules);
		}
		return true;
	}

	public static void setClientMirror(boolean enabled) {
		clientMirror = enabled;
	}
}
