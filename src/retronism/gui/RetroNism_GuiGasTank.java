package retronism.gui;

import net.minecraft.src.*;
import retronism.*;
import aero.machineapi.*;
import retronism.tile.*;
import retronism.container.*;
import org.lwjgl.opengl.GL11;

public class Retronism_GuiGasTank extends GuiContainer {
	private Retronism_TileGasTank tank;
	private int mouseX;
	private int mouseY;
	private Retronism_GuiSideConfigHelper sideConfigHelper;

	public Retronism_GuiGasTank(InventoryPlayer playerInv, Retronism_TileGasTank tank) {
		super(new Retronism_ContainerGasTank(playerInv, tank));
		this.tank = tank;
		this.sideConfigHelper = new Retronism_GuiSideConfigHelper(tank, Retronism_Registry.gasTankBlock.blockID);
	}

	protected void drawGuiContainerForegroundLayer() {
		this.fontRenderer.drawString("Gas Tank", 60, 6, 4210752);
		this.fontRenderer.drawString("Inventory", 8, this.ySize - 96 + 2, 4210752);

		int guiLeft = (this.width - this.xSize) / 2;
		int guiTop = (this.height - this.ySize) / 2;
		int relMouseX = this.mouseX - guiLeft;
		int relMouseY = this.mouseY - guiTop;

		String tooltip = null;
		if (relMouseX >= 7 && relMouseX < 23 && relMouseY >= 16 && relMouseY < 70) {
			String gasName = Aero_GasType.getName(this.tank.getGasType());
			tooltip = gasName + ": " + this.tank.getGasAmount() + " / " + Retronism_TileGasTank.MAX_GAS + " mB";
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
		int textureID = this.mc.renderEngine.getTexture("/gui/retronism_gas_tank.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.renderEngine.bindTexture(textureID);
		int x = (this.width - this.xSize) / 2;
		int y = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);

		int gasScaled = this.tank.getGasScaled(52);
		int gasColor = Aero_GasType.getColor(this.tank.getGasType());
		if (this.tank.getGasType() == Aero_GasType.NONE) gasColor = 0xFF777777;
		drawGasTank(x + 8, y + 17, 14, 52, gasScaled, gasColor);
		drawTankGauge(x + 8, y + 17, 14, 52);
	}

	private void drawGasTank(int x, int y, int w, int h, int fillHeight, int gasColor) {
		if (fillHeight <= 0) return;

		int fillTop = y + h - fillHeight;

		// Grayscale overlay tinted with gas color
		float r = ((gasColor >> 16) & 0xFF) / 255.0F;
		float g = ((gasColor >> 8) & 0xFF) / 255.0F;
		float b = (gasColor & 0xFF) / 255.0F;

		int textureIndex = mod_Retronism.GAS_OVERLAY_INDEX;
		int tx = (textureIndex & 15) << 4;
		int ty = (textureIndex >> 4) << 4;

		int terrainTexture = this.mc.renderEngine.getTexture("/terrain.png");
		GL11.glColor4f(r, g, b, 1.0F);
		this.mc.renderEngine.bindTexture(terrainTexture);

		for (int px = 0; px < w; px += 16) {
			int drawW = Math.min(16, w - px);
			for (int py = fillTop; py < y + h; py += 16) {
				int drawH = Math.min(16, y + h - py);
				this.drawTexturedModalRect(x + px, py, tx, ty, drawW, drawH);
			}
		}

		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
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
