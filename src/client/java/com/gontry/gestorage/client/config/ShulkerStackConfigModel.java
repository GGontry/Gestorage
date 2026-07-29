package com.gontry.gestorage.client.config;

import io.wispforest.owo.config.annotation.Config;

@Config(name = "gestorage/shulker_stack", wrapperName = "ShulkerStackConfig")
public class ShulkerStackConfigModel {
	public boolean enabled = true;
	public boolean stackOnlyEmpty = true;
}
