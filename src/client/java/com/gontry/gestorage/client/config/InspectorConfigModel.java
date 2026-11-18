package com.gontry.gestorage.client.config;

import io.wispforest.owo.config.annotation.Config;

/** Client settings for the crosshair inventory/entity inspector. */
@Config(name = "gestorage/inspector", wrapperName = "InspectorConfig")
public class InspectorConfigModel {
	public boolean enabled = false;
	public boolean toggleMode = false;
	public boolean showInfo = false;
	public String inspectKey = "";
	public String toggleEnabledKey = "";
	public String toggleModeKey = "";
	public String toggleShowInfoKey = "";
}
