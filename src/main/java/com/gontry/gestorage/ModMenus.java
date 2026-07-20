package com.gontry.gestorage;

import com.gontry.gestorage.menu.ExtraLargeEnderMenu;
import com.gontry.gestorage.menu.LargeEnderMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import io.netty.buffer.Unpooled;

public class ModMenus {
	public static ScreenHandlerType<LargeEnderMenu> LARGE_ENDER;
	public static ScreenHandlerType<ExtraLargeEnderMenu> EXTRA_LARGE_ENDER;

	public static void register() {
		LARGE_ENDER = Registry.register(
				Registries.SCREEN_HANDLER,
				Identifier.of(Gestorage.MOD_ID, "large_ender"),
				new ExtendedScreenHandlerType<>((syncId, playerInv, buf) ->
						new LargeEnderMenu(syncId, playerInv),
						PacketCodec.unit(new PacketByteBuf(Unpooled.buffer()))
				)
		);

		EXTRA_LARGE_ENDER = Registry.register(
				Registries.SCREEN_HANDLER,
				Identifier.of(Gestorage.MOD_ID, "extra_large_ender"),
				new ExtendedScreenHandlerType<>((syncId, playerInv, buf) ->
						new ExtraLargeEnderMenu(syncId, playerInv),
						PacketCodec.unit(new PacketByteBuf(Unpooled.buffer()))
				)
		);
	}
}
