package com.gontry.gestorage.client.config;

import io.wispforest.owo.config.annotation.Config;

@Config(name = "gestorage/shulker_refill", wrapperName = "ShulkerRefillConfig")
public class ShulkerRefillConfigModel {
	public boolean enabled = false;
	public String shulkerRefillKey = "";
	public int refillThreshold = 0;
}
