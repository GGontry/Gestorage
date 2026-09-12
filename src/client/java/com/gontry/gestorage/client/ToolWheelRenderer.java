package com.gontry.gestorage.client;

import com.gontry.gestorage.client.config.ModuleConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class ToolWheelRenderer {
	private static final int SLOT_COUNT = 9;
	private static final double START_ANGLE = -90.0;
	private static final double SLOT_ANGLE = 360.0 / SLOT_COUNT;
	private static final int TILE = 18;
	private static final int RADIUS = 42;
	private static final double CANCEL_RADIUS = 12.0;

	private ToolWheelRenderer() {}

	public static void register() {
		HudRenderCallback.EVENT.register((context, tickCounter) -> {
			if (!ClientToolWheelState.wheelActive) return;
			if (!ModuleConfig.toolWheel().enabled()) return;
			render(context);
		});
	}

	private static void render(DrawContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		int cxp = client.getWindow().getScaledWidth() / 2;
		int cyp = client.getWindow().getScaledHeight() / 2;
		int hover = computeHoverSlot(client);
		TextRenderer tr = client.textRenderer;

		for (int i = 0; i < SLOT_COUNT; i++) {
			double rad = Math.toRadians(START_ANGLE + i * SLOT_ANGLE);
			int x = (int) Math.round(cxp + Math.cos(rad) * RADIUS) - TILE / 2;
			int y = (int) Math.round(cyp + Math.sin(rad) * RADIUS) - TILE / 2;
			context.fill(x - 1, y - 1, x + TILE + 1, y + TILE + 1, hover == i ? 0xFFFFFFFF : 0xFF4A4A4A);
			context.fill(x, y, x + TILE, y + TILE, 0xCC000000);
			ItemStack stack = ClientToolWheelState.STACKS[i];
			if (!stack.isEmpty()) {
				context.drawItem(stack, x + 1, y + 1);
				context.drawItemInSlot(tr, stack, x + 1, y + 1, null);
			}
		}

		String label;
		if (hover >= 0) {
			ItemStack stack = ClientToolWheelState.STACKS[hover];
			label = stack.isEmpty() ? "Empty" : stack.getName().getString();
		} else {
			label = "Tool Wheel";
		}
		Text title = Text.literal(label);
		context.drawTextWithShadow(tr, title, cxp - tr.getWidth(title) / 2, cyp - RADIUS - TILE / 2 - tr.fontHeight - 2, 0xFFFFFFFF);
		Text hint = Text.literal("LMB: select / RMB: cancel");
		context.drawTextWithShadow(tr, hint, cxp - tr.getWidth(hint) / 2, cyp + RADIUS + 10, 0xFFAAAAAA);
	}

	public static int computeHoverSlot(MinecraftClient client) {
		double sw = client.getWindow().getScaledWidth();
		double sh = client.getWindow().getScaledHeight();
		double mx = client.mouse.getX() * sw / client.getWindow().getWidth();
		double my = client.mouse.getY() * sh / client.getWindow().getHeight();
		double dx = mx - sw / 2;
		double dy = my - sh / 2;
		double dist = Math.sqrt(dx * dx + dy * dy);
		if (dist < CANCEL_RADIUS) return -1;
		double rel = Math.toDegrees(Math.atan2(dy, dx)) - START_ANGLE;
		rel = ((rel % 360) + 360) % 360;
		return (int) Math.floor(rel / SLOT_ANGLE) % SLOT_COUNT;
	}
}