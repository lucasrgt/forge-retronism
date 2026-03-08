package retronism.gui;

import net.minecraft.src.*;
import retronism.*;
import retronism.tile.*;
import retronism.container.*;
import org.lwjgl.opengl.GL11;

public class Retronism_GuiGenerator extends GuiContainer {
	private Retronism_TileGenerator generator;
	private int mouseX;
	private int mouseY;
	private Retronism_GuiSideConfigHelper sideConfigHelper;

	public Retronism_GuiGenerator(InventoryPlayer playerInv, Retronism_TileGenerator generator) {
		super(new Retronism_ContainerGenerator(playerInv, generator));
		this.generator = generator;
		this.sideConfigHelper = new Retronism_GuiSideConfigHelper(generator, Retronism_Registry.generatorBlock.blockID);
	}

	protected void drawGuiContainerForegroundLayer() {
		this.fontRenderer.drawString("Generator", 60, 6, 4210752);
		this.fontRenderer.drawString("Inventory", 8, this.ySize - 96 + 2, 4210752);

		// Energy bar tooltip (coordinates relative to GUI origin)
		int guiLeft = (this.width - this.xSize) / 2;
		int guiTop = (this.height - this.ySize) / 2;
		int relMouseX = this.mouseX - guiLeft;
		int relMouseY = this.mouseY - guiTop;
		// Energy bar outer area in GUI coords
		if (relMouseX >= 161 && relMouseX < 169 && relMouseY >= 16 && relMouseY < 70) {
			String line1 = "Energy: " + this.generator.storedEnergy + " / " + Retronism_TileGenerator.MAX_ENERGY + " RN";
			String line2 = this.generator.isBurning() ? "Generating: 32 RN/t" : null;
			String line3 = this.generator.lastOutput > 0 ? "Output: " + this.generator.lastOutput + " RN/t" : null;

			int lines = 1;
			int maxW = this.fontRenderer.getStringWidth(line1);
			if (line2 != null) { lines++; maxW = Math.max(maxW, this.fontRenderer.getStringWidth(line2)); }
			if (line3 != null) { lines++; maxW = Math.max(maxW, this.fontRenderer.getStringWidth(line3)); }

			int tx = relMouseX - maxW - 15;
			int ty = relMouseY - 12;
			int h = 8 + (lines - 1) * 10;
			this.drawGradientRect(tx - 3, ty - 3, tx + maxW + 3, ty + h + 3, -1073741824, -1073741824);
			this.fontRenderer.drawStringWithShadow(line1, tx, ty, -1);
			int lineY = ty + 10;
			if (line2 != null) { this.fontRenderer.drawStringWithShadow(line2, tx, lineY, -1); lineY += 10; }
			if (line3 != null) { this.fontRenderer.drawStringWithShadow(line3, tx, lineY, -1); }
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
		int textureID = this.mc.renderEngine.getTexture("/gui/retronism_generator.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.renderEngine.bindTexture(textureID);
		int x = (this.width - this.xSize) / 2;
		int y = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);

		// Energy bar fill
		Retronism_GuiUtils.drawEnergyBar(x + 162, y + 17, 6, 52, Retronism_GuiUtils.getEnergyScaled(this.generator.storedEnergy, Retronism_TileGenerator.MAX_ENERGY, 52));
	}
}
