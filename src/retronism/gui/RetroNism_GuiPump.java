package retronism.gui;

import net.minecraft.src.*;
import retronism.*;
import retronism.tile.*;
import retronism.container.*;
import org.lwjgl.opengl.GL11;

public class Retronism_GuiPump extends GuiContainer {
	private Retronism_TilePump pump;
	private int mouseX;
	private int mouseY;
	private Retronism_GuiSideConfigHelper sideConfigHelper;

	public Retronism_GuiPump(InventoryPlayer playerInv, Retronism_TilePump pump) {
		super(new Retronism_ContainerPump(playerInv, pump));
		this.pump = pump;
		this.sideConfigHelper = new Retronism_GuiSideConfigHelper(pump, Retronism_Registry.pumpBlock.blockID);
	}

	protected void drawGuiContainerForegroundLayer() {
		this.fontRenderer.drawString("Water Pump", 52, 6, 4210752);
		this.fontRenderer.drawString("Inventory", 8, this.ySize - 96 + 2, 4210752);

		int guiLeft = (this.width - this.xSize) / 2;
		int guiTop = (this.height - this.ySize) / 2;
		int relMouseX = this.mouseX - guiLeft;
		int relMouseY = this.mouseY - guiTop;

		String tooltip = null;
		if (relMouseX >= 7 && relMouseX < 23 && relMouseY >= 16 && relMouseY < 70) {
			tooltip = "Water: " + this.pump.fluidAmount + " / " + Retronism_TilePump.MAX_FLUID + " mB";
		} else if (relMouseX >= 161 && relMouseX < 169 && relMouseY >= 16 && relMouseY < 70) {
			tooltip = "Energy: " + this.pump.storedEnergy + " / " + Retronism_TilePump.MAX_ENERGY + " RN";
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
		int guiLeft = (this.width - this.xSize) / 2;
		int guiTop = (this.height - this.ySize) / 2;
		this.sideConfigHelper.drawTabs(guiLeft, guiTop, this.fontRenderer, this.mc.renderEngine);
		if (this.sideConfigHelper.isConfigMode()) {
			this.sideConfigHelper.drawConfigOverlay(guiLeft, guiTop, this.xSize, this.ySize, this.fontRenderer, mouseX, mouseY, this.mc.renderEngine);
		}
	}

	protected void mouseClicked(int mouseX, int mouseY, int button) {
		int guiLeft = (this.width - this.xSize) / 2;
		int guiTop = (this.height - this.ySize) / 2;
		if (this.sideConfigHelper.handleClick(mouseX, mouseY, guiLeft, guiTop, this.xSize, this.ySize, this.fontRenderer)) return;
		if (this.sideConfigHelper.isConfigMode()) return;
		super.mouseClicked(mouseX, mouseY, button);
	}

	protected void drawGuiContainerBackgroundLayer(float partialTick) {
		int textureID = this.mc.renderEngine.getTexture("/gui/retronism_pump.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.renderEngine.bindTexture(textureID);
		int x = (this.width - this.xSize) / 2;
		int y = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);

		// Fluid tank fill (blue, left)
		int fluidX = x + 8;
		int fluidY = y + 17;
		int fluidW = 14;
		int fluidH = 52;
		int fluidScaled = this.pump.getFluidScaled(fluidH);
		drawLiquidTank(fluidX, fluidY, fluidW, fluidH, fluidScaled, Block.waterMoving.blockIndexInTexture);
		drawTankGauge(fluidX, fluidY, fluidW, fluidH);

		// Energy bar fill
		Retronism_GuiUtils.drawEnergyBar(x + 162, y + 17, 6, 52, Retronism_GuiUtils.getEnergyScaled(this.pump, 52));
	}

	private void drawLiquidTank(int x, int y, int w, int h, int fillHeight, int textureIndex) {
		if (fillHeight <= 0) return;

		int fillTop = y + h - fillHeight;
		int tx = (textureIndex & 15) << 4;
		int ty = (textureIndex >> 4) << 4;

		int terrainTexture = this.mc.renderEngine.getTexture("/terrain.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.renderEngine.bindTexture(terrainTexture);

		for (int px = 0; px < w; px += 16) {
			int drawW = Math.min(16, w - px);
			for (int py = fillTop; py < y + h; py += 16) {
				int drawH = Math.min(16, y + h - py);
				this.drawTexturedModalRect(x + px, py, tx, ty, drawW, drawH);
			}
		}
	}

	private void drawTankGauge(int x, int y, int w, int h) {
		int totalLines = h / 5 - 1;
		if (totalLines < 1) return;
		int halfW = w / 2;
		for (int i = 1; i <= totalLines; i++) {
			int ly = y + (h * i / (totalLines + 1));
			if (i % 5 == 0) {
				drawRect(x, ly, x + w, ly + 1, 0xFF560001);
			} else {
				drawRect(x, ly, x + halfW, ly + 1, 0xFF560001);
			}
		}
	}
}
