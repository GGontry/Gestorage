package com.gontry.gestorage.client.config;

import io.wispforest.owo.config.annotation.Config;

@Config(name = "gestorage/inventory_sorting", wrapperName = "InventorySortingConfig")
public class InventorySortingConfigModel {
	public boolean enabled = false;
	public boolean showButtons = true;
	public String sortKey = "";

	public boolean mergeStacks = true;
	public boolean sortByName = true;
	public boolean sortDescending = false;

	public boolean blockPlayer = false;
	public boolean blockEnderChest = false;
	public boolean blockShulkerBox = false;
	public boolean blockGenericContainer = false;

	public String toggleEnabledKey = "";
	public String toggleShowButtonsKey = "";
	public String toggleMergeStacksKey = "";
	public String toggleSortByNameKey = "";
	public String toggleSortDescendingKey = "";
	public String toggleBlockPlayerKey = "";
	public String toggleBlockEnderChestKey = "";
	public String toggleBlockShulkerBoxKey = "";
	public String toggleBlockGenericContainerKey = "";
}
