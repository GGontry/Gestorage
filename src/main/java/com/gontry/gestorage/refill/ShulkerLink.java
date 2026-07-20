package com.gontry.gestorage.refill;

public record ShulkerLink(
	int sourceSlot,
	String sourceType,
	int targetSlot,
	String targetType
) {
	public boolean involvesSlot(int slot, String type) {
		return sourceSlot == slot && sourceType.equals(type)
			|| targetSlot == slot && targetType.equals(type);
	}
}
