package com.gontry.gestorage.client;

public class ClientCarefulBreakState {
	public static volatile boolean enabled = false;
	public static volatile boolean carefulBreak = false;
	public static volatile boolean carefulDrop = false;
	public static volatile boolean alwaysCareful = false;
	public static volatile boolean treeCapitator = false;
	public static volatile boolean betterHarvesting = false;
	public static volatile boolean autoReplant = false;

	private ClientCarefulBreakState() {}

	public static void apply(boolean enabled, boolean carefulBreak, boolean carefulDrop,
			boolean alwaysCareful, boolean treeCapitator, boolean betterHarvesting,
			boolean autoReplant) {
		ClientCarefulBreakState.enabled = enabled;
		ClientCarefulBreakState.carefulBreak = carefulBreak;
		ClientCarefulBreakState.carefulDrop = carefulDrop;
		ClientCarefulBreakState.alwaysCareful = alwaysCareful;
		ClientCarefulBreakState.treeCapitator = treeCapitator;
		ClientCarefulBreakState.betterHarvesting = betterHarvesting;
		ClientCarefulBreakState.autoReplant = autoReplant;
	}
}
