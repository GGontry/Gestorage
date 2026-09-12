package com.gontry.gestorage.client.config;

import io.wispforest.owo.config.annotation.Config;

@Config(name = "gestorage/tool_wheel", wrapperName = "ToolWheelConfig")
public class ToolWheelConfigModel {
	public boolean enabled = true;
	public String openWheelKey = "";
	public String wheelKey = "";
	public String autoToolKey = "";
	public String toggleEnabledKey = "";
}