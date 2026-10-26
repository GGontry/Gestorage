package com.gontry.gestorage.client.config;

import io.wispforest.owo.config.annotation.Config;

/**
 * Slot Lock settings. {@code lockKey} is held while clicking a slot to toggle its
 * lock; the three behaviour switches are mirrored to the server so enforcement
 * stays server-authoritative.
 */
@Config(name = "gestorage/slot_lock", wrapperName = "SlotLockConfig")
public class SlotLockConfigModel {
	public boolean enabled = false;
	public String lockKey = "";

	public boolean sortingSkipsLockedSlots = true;
	public boolean preventDrop = true;
	public boolean preventUse = true;
}
