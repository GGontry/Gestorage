package com.gontry.gestorage.screen;

import com.gontry.gestorage.Gestorage;
import com.gontry.gestorage.menu.ExtraLargeEnderMenu;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ExtraLargeEnderScreen extends HandledScreen<ExtraLargeEnderMenu> {
	private static final Identifier TEXTURE = Identifier.of(Gestorage.MOD_ID, "textures/gui/container/extra_large_ender.png");
	private static final int CHEST_ROWS = 12;
	private static final int CHEST_COLS = 19;
	private static final int TEX_WIDTH = 356;
	private static final int TEX_HEIGHT = 330;

	public ExtraLargeEnderScreen(ExtraLargeEnderMenu handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = TEX_WIDTH;
		this.backgroundHeight = TEX_HEIGHT;
		this.playerInventoryTitleY = this.backgroundHeight - 94;
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		int x = (this.width - this.backgroundWidth) / 2;
		int y = (this.height - this.backgroundHeight) / 2;

		int chestAreaHeight = CHEST_ROWS * 18 + 17;
		int playerInvUV = CHEST_ROWS * 18 + 18;

		context.drawTexture(TEXTURE, x, y, 0, 0.0f, 0.0f,
				this.backgroundWidth, chestAreaHeight, TEX_WIDTH, TEX_HEIGHT);

		context.drawTexture(TEXTURE, x, y + chestAreaHeight, 0,
				0.0f, (float) playerInvUV,
				this.backgroundWidth, 96, TEX_WIDTH, TEX_HEIGHT);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}
}
