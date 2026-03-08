package retronism.gui;

import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;
import retronism.tile.Retronism_TileMegaElectrolysis;
import retronism.container.Retronism_ContainerMegaElectrolysis;

public class Retronism_GuiMegaElectrolysis extends GuiContainer {

	private Retronism_TileMegaElectrolysis tile;

	public Retronism_GuiMegaElectrolysis(InventoryPlayer playerInv, Retronism_TileMegaElectrolysis tile) {
		super(new Retronism_ContainerMegaElectrolysis(playerInv, tile));
		this.tile = tile;
		this.xSize = 176;
		this.ySize = 166;
	}

	protected void drawGuiContainerForegroundLayer() {
		String title = "Mega Electrolysis";
		fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 6, 4210752);
		fontRenderer.drawString("Inventory", 8, ySize - 96 + 2, 4210752);

		if (!tile.isFormed) {
			fontRenderer.drawString("Structure incomplete!", 8, 20, 0xFF4444);
		}
	}

	protected void drawGuiContainerBackgroundLayer(float partialTicks) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		int texId = this.mc.renderEngine.getTexture("/gui/retronism_megaelectrolysis.png");
		this.mc.renderEngine.bindTexture(texId);
		int x = (width - xSize) / 2;
		int y = (height - ySize) / 2;
		this.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);

		// Energy bar fill (right side)
		int barX = x + 162, barY = y + 17, barW = 6, barH = 52;
		int energyScaled = tile.getEnergyScaled(barH);
		if (energyScaled > 0) {
			int top = barY + barH - energyScaled;
			for (int sy = top; sy < barY + barH; sy++) {
				int color = (sy % 2 == 0) ? 0xFF3BFB98 : 0xFF36E38A;
				drawRect(barX, sy, barX + barW, sy + 1, color);
			}
		}

		// Progress arrow
		int cookScale = tile.getCookProgressScaled(24);
		if (cookScale > 0) {
			this.drawTexturedModalRect(x + 52, y + 34, 176, 14, cookScale + 1, 17);
		}

		// Water tank (left, blue)
		int waterScaled = tile.getWaterScaled(52);
		if (waterScaled > 0) {
			drawRect(x + 13, y + 17 + 52 - waterScaled, x + 13 + 14, y + 17 + 52, 0xFF3344FF);
		}

		// Hydrogen tank (top right, light blue)
		int h2Scaled = tile.getHydrogenScaled(22);
		if (h2Scaled > 0) {
			drawRect(x + 101, y + 17 + 22 - h2Scaled, x + 101 + 14, y + 17 + 22, 0xFF88BBFF);
		}

		// Oxygen tank (bottom right, red-ish)
		int o2Scaled = tile.getOxygenScaled(22);
		if (o2Scaled > 0) {
			drawRect(x + 101, y + 47 + 22 - o2Scaled, x + 101 + 14, y + 47 + 22, 0xFFFF8888);
		}

		// Heavy Water tank (far right, dark blue)
		int hwScaled = tile.getHeavyWaterScaled(52);
		if (hwScaled > 0) {
			drawRect(x + 137, y + 17 + 52 - hwScaled, x + 137 + 14, y + 17 + 52, 0xFF1A237E);
		}
	}
}
