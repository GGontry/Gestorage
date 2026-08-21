package com.gontry.gestorage.client.config;

import io.wispforest.owo.config.annotation.Config;

@Config(name = "gestorage/storage_overlay", wrapperName = "StorageOverlayConfig")
public class StorageOverlayConfigModel {
	public boolean enabled = false;
	public boolean showInventoryName = true;
	public boolean showItemName = true;
	public boolean showItemIcon = true;
	public boolean showStackCount = true;
	public boolean showItemCount = true;
	public boolean showFreeSlots = true;
	public int offsetX = 0;
	public int offsetY = 0;

	public String toggleEnabledKey = "";
	public String toggleInventoryNameKey = "";
	public String toggleItemNameKey = "";
	public String toggleItemIconKey = "";
	public String toggleStackCountKey = "";
	public String toggleItemCountKey = "";
	public String toggleFreeSlotsKey = "";
}
