package com.gontry.gestorage.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
	@Accessor("focusedSlot")
	Slot gestorage_getFocusedSlot();

	@Invoker("getSlotAt")
	Slot gestorage_getSlotAt(double x, double y);

	@Accessor("x")
	int gestorage_getX();

	@Accessor("y")
	int gestorage_getY();

	@Accessor("backgroundWidth")
	int gestorage_getBackgroundWidth();

	@Accessor("backgroundHeight")
	int gestorage_getBackgroundHeight();
}
