package com.gontry.gestorage.client;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.client.config.ModuleConfig;
import com.gontry.gestorage.client.ui.ConfigTextures;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Draws the inspected inventory grid and, optionally, the target info box above it.
 * Both panels share one width and the same center X, so the HUD stays symmetric
 * whatever the target's name or info lines are. Panels reuse the mod's translucent
 * {@code panel.png} instead of flat colour fills. The grid itself has a single
 * frame around all slots, so items are not split into separate little squares.
 */
public class InspectorRenderer {
	private static final Identifier HUD_LAYER = Identifier.of(Gestorage.MOD_ID, "inspector");
	private static final Identifier PANEL_TEXTURE = Identifier.of(Gestorage.MOD_ID, "textures/gui/overlay/panel.png");
	private static final int COLS = 9;
	private static final int SLOT = 18;
	private static final int MAX_SLOTS = 81;
	private static final int ICON = 16;
	private static final int ICON_GAP = 4;
	private static final int LINE_H = 10;
	private static final int PAD_X = 5;
	private static final int PAD_Y = 4;
	private static final int PANEL_GAP = 3;
	private static final int TITLE_GAP = 2;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int SUB_COLOR = 0xFFC8C8C8;
	private static final int EMPTY_COLOR = 0xFF9A9A9A;

	private InspectorRenderer() {}

	public static void register() {
		HudElementRegistry.addLast(HUD_LAYER, (context, tickCounter) -> {
			if (!ModuleConfig.inspector().enabled() || !ClientInspectorState.active) return;
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player == null || client.world == null || client.currentScreen != null) return;
			render(context, client);
		});
	}

	private static void render(DrawContext context, MinecraftClient client) {
		TextRenderer tr = client.textRenderer;
		int scaledW = client.getWindow().getScaledWidth();
		int scaledH = client.getWindow().getScaledHeight();
		List<ItemStack> stacks = ClientInspectorState.stacks;
		List<String> info = ModuleConfig.inspector().showInfo() ? ClientInspectorState.infoLines : List.of();
		int shown = Math.min(stacks.size(), MAX_SLOTS);
		int rows = Math.max(1, (shown + COLS - 1) / COLS);
		int gridW = COLS * SLOT;
		int gridH = rows * SLOT;

		boolean hasIcon = !ClientInspectorState.icon.isEmpty();
		int iconAndGap = hasIcon ? ICON + ICON_GAP : 0;
		String rawTitle = ClientInspectorState.loading && ClientInspectorState.title.isEmpty()
				? "Inspecting..." : ClientInspectorState.title;
		int infoW = 0;
		for (String line : info) {
			infoW = Math.max(infoW, tr.getWidth(Text.literal(line)));
		}
		int panelW = Math.min(
				Math.max(Math.max(gridW, tr.getWidth(Text.literal(rawTitle)) + iconAndGap), infoW) + PAD_X * 2,
				Math.max(gridW + PAD_X * 2, scaledW - 8)
		);
		int textW = panelW - PAD_X * 2;
		String titleText = tr.trimToWidth(rawTitle, Math.max(0, textW - iconAndGap));
		Text title = Text.literal(titleText);
		int titleW = tr.getWidth(title) + iconAndGap;

		int titleH = hasIcon ? ICON : tr.fontHeight;
		int invH = PAD_Y * 2 + titleH + TITLE_GAP + gridH;
		int infoH = info.isEmpty() ? 0 : info.size() * LINE_H + PAD_Y * 2;
		int totalH = infoH + (infoH > 0 ? PANEL_GAP : 0) + invH;

		int x = (scaledW - panelW) / 2;
		int y = Math.max(4, (scaledH - totalH) / 2);

		if (infoH > 0) {
			ConfigTextures.drawNineSlice(context, PANEL_TEXTURE, x, y, panelW, infoH);
			for (int i = 0; i < info.size(); i++) {
				Text line = Text.literal(tr.trimToWidth(info.get(i), Math.max(0, textW)));
				int lineX = x + (panelW - tr.getWidth(line)) / 2;
				context.drawTextWithShadow(tr, line, lineX, y + PAD_Y + i * LINE_H,
						i == 0 ? TEXT_COLOR : SUB_COLOR);
			}
			y += infoH + PANEL_GAP;
		}

		ConfigTextures.drawNineSlice(context, PANEL_TEXTURE, x, y, panelW, invH);
		int rowY = y + PAD_Y;
		int titleX = x + (panelW - titleW) / 2;
		if (hasIcon) {
			context.drawItem(ClientInspectorState.icon, titleX, rowY);
			context.drawTextWithShadow(tr, title, titleX + ICON + ICON_GAP,
					rowY + (ICON - tr.fontHeight) / 2, TEXT_COLOR);
		} else {
			context.drawTextWithShadow(tr, title, titleX, rowY, TEXT_COLOR);
		}

		int gridX = x + (panelW - gridW) / 2;
		int gridY = rowY + titleH + TITLE_GAP;
		// One frame for the whole grid: no border between individual slots, just
		// the panel's translucent fill behind the items and one edge all around.
		ConfigTextures.drawNineSlice(context, PANEL_TEXTURE, gridX, gridY, gridW, gridH);
		if (shown == 0) {
			Text status = ClientInspectorState.loading ? Text.literal("Loading...") : Text.literal("Empty");
			context.drawTextWithShadow(tr, status, x + (panelW - tr.getWidth(status)) / 2,
					gridY + (gridH - tr.fontHeight) / 2, EMPTY_COLOR);
		}
		for (int i = 0; i < shown; i++) {
			int sx = gridX + (i % COLS) * SLOT;
			int sy = gridY + (i / COLS) * SLOT;
			ItemStack stack = stacks.get(i);
			if (!stack.isEmpty()) {
				context.drawItem(stack, sx + 1, sy + 1);
				context.drawStackOverlay(tr, stack, sx + 1, sy + 1, null);
			}
		}
	}
}
