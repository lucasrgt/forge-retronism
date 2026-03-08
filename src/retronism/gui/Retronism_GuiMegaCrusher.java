package retronism.gui;

import net.minecraft.src.*;
import retronism.*;
import retronism.tile.*;
import retronism.container.*;
import org.lwjgl.opengl.GL11;

public class Retronism_GuiMegaCrusher extends GuiContainer {
	private Retronism_TileMegaCrusher megaCrusher;
	private int mouseX;
	private int mouseY;
	private Retronism_GuiSideConfigHelper sideConfigHelper;

	public Retronism_GuiMegaCrusher(InventoryPlayer playerInv, Retronism_TileMegaCrusher megaCrusher) {
		super(new Retronism_ContainerMegaCrusher(playerInv, megaCrusher));
		this.megaCrusher = megaCrusher;
		this.sideConfigHelper = new Retronism_GuiSideConfigHelper(megaCrusher, Retronism_Registry.megaCrusherCoreBlock.blockID);
	}

	protected void drawGuiContainerForegroundLayer() {
		this.fontRenderer.drawString("Mega Crusher", 44, 6, 4210752);
		this.fontRenderer.drawString("Inventory", 8, this.ySize - 96 + 2, 4210752);

		int guiLeft = (this.width - this.xSize) / 2;
		int guiTop = (this.height - this.ySize) / 2;
		int relMouseX = this.mouseX - guiLeft;
		int relMouseY = this.mouseY - guiTop;
		if (relMouseX >= 161 && relMouseX < 169 && relMouseY >= 16 && relMouseY < 70) {
			String line1 = "Energy: " + this.megaCrusher.storedEnergy + " / " + Retronism_TileMegaCrusher.MAX_ENERGY + " RN";
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
		int textureID = this.mc.renderEngine.getTexture("/gui/retronism_mega_crusher.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.renderEngine.bindTexture(textureID);
		int x = (this.width - this.xSize) / 2;
		int y = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);

		// 3 progress arrows (all use same filled sprite at 176,14)
		for (int i = 0; i < 3; i++) {
			int cookScale = this.megaCrusher.getCookProgressScaled(i, 24);
			if (cookScale > 0) {
				int arrowY = 17 + i * 22;
				this.drawTexturedModalRect(x + 82, y + arrowY, 176, 14, cookScale + 1, 17);
			}
		}

		// Energy bar fill
		Retronism_GuiUtils.drawEnergyBar(x + 162, y + 17, 6, 52, Retronism_GuiUtils.getEnergyScaled(this.megaCrusher, 52));
	}
}
