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
	private static volatile boolean clientHoppersMirror = true;

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
	 * Whether hoppers may treat empty shulkers as stackable. The client mirror
	 * matters for GUI prediction: the server and the client must agree on the
	 * hopper slot limit, or a shift-click shows a stack the server then rejects.
	 */
	public static boolean hoppersAllowed() {
		GameRules rules = serverRules;
		if (rules != null) {
			return ModGameRules.isHopperStackableShulkers(rules);
		}
		return clientHoppersMirror;
	}

	public static void setClientMirror(boolean enabled, boolean hoppersAllowed) {
		clientMirror = enabled;
		clientHoppersMirror = hoppersAllowed;
	}

	/** Drops the mirrored state when leaving a world. */
	public static void resetClientMirror() {
		clientMirror = false;
		clientHoppersMirror = true;
	}
}
