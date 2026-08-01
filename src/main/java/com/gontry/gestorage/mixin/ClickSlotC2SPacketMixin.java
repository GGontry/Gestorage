package com.gontry.gestorage.mixin;

import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ClickSlotC2SPacket.class)
public class ClickSlotC2SPacketMixin {
	private static final int MAX_MODIFIED_STACKS = 512;

	@ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 128))
	private static int gestorage$raiseModifiedStackLimit(int original) {
		return MAX_MODIFIED_STACKS;
	}
}
