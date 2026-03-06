package retronism.gui;

import net.minecraft.src.*;
import retronism.tile.*;
import retronism.container.*;
import org.lwjgl.opengl.GL11;

public class RetroNism_GuiCrusher extends GuiContainer {
	private RetroNism_TileCrusher crusherInventory;
	private int mouseX;
	private int mouseY;

	public RetroNism_GuiCrusher(InventoryPlayer playerInv, RetroNism_TileCrusher crusher) {
		super(new RetroNism_ContainerCrusher(playerInv, crusher));
		this.crusherInventory = crusher;
	}

	protected void drawGuiContainerForegroundLayer() {
		this.fontRenderer.drawString("Crusher", 60, 6, 4210752);
		this.fontRenderer.drawString("Inventory", 8, this.ySize - 96 + 2, 4210752);

		// Energy bar tooltip
		int guiLeft = (this.width - this.xSize) / 2;
		int guiTop = (this.height - this.ySize) / 2;
		int relMouseX = this.mouseX - guiLeft;
		int relMouseY = this.mouseY - guiTop;
		if (relMouseX >= 161 && relMouseX < 169 && relMouseY >= 16 && relMouseY < 70) {
			String line1 = "Energy: " + this.crusherInventory.storedEnergy + " / " + RetroNism_TileCrusher.MAX_ENERGY + " RN";
			int maxW = this.fontRenderer.getStringWidth(line1);
			int tx = relMouseX - maxW - 15;
			int ty = relMouseY - 12;
			this.drawGradientRect(tx - 3, ty - 3, tx + maxW + 3, ty + 11, -1073741824, -1073741824);
			this.fontRenderer.drawStringWithShadow(line1, tx, ty, -1);
		}
	}

	public void drawScreen(int mouseX, int mouseY, float partialTick) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		super.drawScreen(mouseX, mouseY, partialTick);
	}

	protected void drawGuiContainerBackgroundLayer(float partialTick) {
		int textureID = this.mc.renderEngine.getTexture("/gui/retronism_crusher.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.renderEngine.bindTexture(textureID);
		int x = (this.width - this.xSize) / 2;
		int y = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);

		// Progress arrow fill (from own texture sprite area at 176,14)
		int cookScale = this.crusherInventory.getCookProgressScaled(24);
		if (cookScale > 0) {
			this.drawTexturedModalRect(x + 82, y + 34, 176, 14, cookScale + 1, 17);
		}

		// Energy bar fill (striped green, same style as Generator)
		int barX = x + 162;
		int barY = y + 17;
		int barW = 6;
		int barH = 52;
		int energyScaled = this.crusherInventory.getEnergyScaled(barH);
		if (energyScaled > 0) {
			int fillTop = barY + barH - energyScaled;
			for (int sy = fillTop; sy < barY + barH; sy++) {
				int color = (sy % 2 == 0) ? 0xFF3BFB98 : 0xFF36E38A;
				drawRect(barX, sy, barX + barW, sy + 1, color);
			}
		}
	}
}
