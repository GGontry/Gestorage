package com.gontry.gestorage.client.config;

import io.wispforest.owo.config.annotation.Config;

@Config(name = "gestorage/ender_chest", wrapperName = "EnderChestConfig")
public class EnderChestConfigModel {
	public boolean enabled = true;
	public String openEnderChestKey = "";
}
