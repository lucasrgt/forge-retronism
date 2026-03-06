package retronism.gui;

import net.minecraft.src.*;
import retronism.tile.*;
import retronism.container.*;
import org.lwjgl.opengl.GL11;

public class RetroNism_GuiElectrolysis extends GuiContainer {
	private RetroNism_TileElectrolysis tile;
	private int mouseX;
	private int mouseY;

	public RetroNism_GuiElectrolysis(InventoryPlayer playerInv, RetroNism_TileElectrolysis tile) {
		super(new RetroNism_ContainerElectrolysis(playerInv, tile));
		this.tile = tile;
	}

	protected void drawGuiContainerForegroundLayer() {
		this.fontRenderer.drawString("Electrolysis", 52, 6, 4210752);
		this.fontRenderer.drawString("Inventory", 8, this.ySize - 96 + 2, 4210752);

		int guiLeft = (this.width - this.xSize) / 2;
		int guiTop = (this.height - this.ySize) / 2;
		int relMouseX = this.mouseX - guiLeft;
		int relMouseY = this.mouseY - guiTop;

		// Tooltips for tanks
		String tooltip = null;
		if (relMouseX >= 12 && relMouseX < 26 && relMouseY >= 14 && relMouseY < 70) {
			tooltip = "Energy: " + tile.storedEnergy + " / " + RetroNism_TileElectrolysis.MAX_ENERGY + " RN";
		} else if (relMouseX >= 34 && relMouseX < 50 && relMouseY >= 14 && relMouseY < 70) {
			tooltip = "Water: " + tile.waterStored + " / " + RetroNism_TileElectrolysis.MAX_WATER + " mB";
		} else if (relMouseX >= 108 && relMouseX < 124 && relMouseY >= 14 && relMouseY < 70) {
			tooltip = "Hydrogen: " + tile.hydrogenStored + " / " + RetroNism_TileElectrolysis.MAX_HYDROGEN + " mB";
		} else if (relMouseX >= 132 && relMouseX < 148 && relMouseY >= 14 && relMouseY < 70) {
			tooltip = "Oxygen: " + tile.oxygenStored + " / " + RetroNism_TileElectrolysis.MAX_OXYGEN + " mB";
		}

		if (tooltip != null) {
			int tw = this.fontRenderer.getStringWidth(tooltip);
			int tx = relMouseX - tw - 5;
			if (tx < 0) tx = relMouseX + 12;
			int ty = relMouseY - 12;
			this.drawGradientRect(tx - 3, ty - 3, tx + tw + 3, ty + 11, -1073741824, -1073741824);
			this.fontRenderer.drawStringWithShadow(tooltip, tx, ty, -1);
		}
	}

	public void drawScreen(int mouseX, int mouseY, float partialTick) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		super.drawScreen(mouseX, mouseY, partialTick);
	}

	protected void drawGuiContainerBackgroundLayer(float partialTick) {
		int textureID = this.mc.renderEngine.getTexture("/gui/furnace.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.renderEngine.bindTexture(textureID);
		int x = (this.width - this.xSize) / 2;
		int y = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);

		// Cover furnace slots area with background color
		int bg = 0xFFC6C6C6;
		drawRect(x + 8, y + 14, x + 168, y + 72, bg);

		// Energy bar (red)
		drawTank(x + 12, y + 14, 14, 56, tile.getEnergyScaled(56), 0xFFCC0000);

		// Water tank (blue)
		drawTank(x + 34, y + 14, 16, 56, tile.getWaterScaled(56), 0xFF3344FF);

		// Progress arrow
		this.mc.renderEngine.bindTexture(textureID);
		this.drawTexturedModalRect(x + 76, y + 35, 176, 14, 24, 16);
		int progressScaled = tile.getProcessScaled(24);
		this.drawTexturedModalRect(x + 76, y + 35, 176, 14, progressScaled + 1, 16);

		// Hydrogen tank (light blue)
		drawTank(x + 108, y + 14, 16, 56, tile.getHydrogenScaled(56), 0xFF88BBFF);

		// Oxygen tank (light red)
		drawTank(x + 132, y + 14, 16, 56, tile.getOxygenScaled(56), 0xFFFF8888);
	}

	private void drawTank(int x, int y, int w, int h, int fillHeight, int fillColor) {
		// Background
		drawRect(x, y, x + w, y + h, 0xFF333333);
		// Fill
		if (fillHeight > 0) {
			drawRect(x + 1, y + h - fillHeight, x + w - 1, y + h, fillColor);
		}
		// Border
		drawRect(x, y, x + w, y + 1, 0xFF666666);
		drawRect(x, y + h - 1, x + w, y + h, 0xFF666666);
		drawRect(x, y, x + 1, y + h, 0xFF666666);
		drawRect(x + w - 1, y, x + w, y + h, 0xFF666666);
	}
}
