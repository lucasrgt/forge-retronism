package retronism.gui;

import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;
import retronism.tile.Retronism_TileOzonizer;
import retronism.container.Retronism_ContainerOzonizer;

public class Retronism_GuiOzonizer extends GuiContainer {

    private Retronism_TileOzonizer tile;
    private int textureID;
    private int mouseX;
    private int mouseY;

    public Retronism_GuiOzonizer(InventoryPlayer playerInv, Retronism_TileOzonizer tile) {
        super(new Retronism_ContainerOzonizer(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    @Override
    protected void drawGuiContainerForegroundLayer() {
        fontRenderer.drawString("Ozonizer", (xSize - fontRenderer.getStringWidth("Ozonizer")) / 2, 6, 4210752);
        fontRenderer.drawString("Inventory", 8, ySize - 96 + 2, 4210752);

        if (!tile.isFormed) {
            fontRenderer.drawString("Structure incomplete!", 8, 20, 0xFF4444);
            return;
        }

        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;
        int relMouseX = this.mouseX - guiLeft;
        int relMouseY = this.mouseY - guiTop;

        String tooltip = null;

        if (relMouseX >= 7 && relMouseX < 15 && relMouseY >= 16 && relMouseY < 70) {
            tooltip = "Energy: " + tile.getStoredEnergy() + " / " + tile.getMaxEnergy() + " RN";
        }
        else if (relMouseX >= 56 && relMouseX < 70 && relMouseY >= 16 && relMouseY < 68) {
            tooltip = "Fluid: " + tile.getFluidAmount() + " / " + tile.getFluidCapacity() + " mB";
        }
        else if (relMouseX >= 112 && relMouseX < 126 && relMouseY >= 16 && relMouseY < 68) {
            tooltip = "Gas: " + tile.getGasAmount() + " / " + tile.getGasCapacity() + " mB";
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

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        textureID = this.mc.renderEngine.getTexture("/gui/retronism_ozonizer.png");
        this.mc.renderEngine.bindTexture(textureID);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);

        // Energy bar fill
        int barX = x + 8, barY = y + 17, barW = 6, barH = 52;
        int scaled = tile.getEnergyScaled(barH);
        if (scaled > 0) {
            int top = barY + barH - scaled;
            for (int sy = top; sy < barY + barH; sy++) {
                int color = (sy % 2 == 0) ? 0xFF3BFB98 : 0xFF36E38A;
                drawRect(barX, sy, barX + barW, sy + 1, color);
            }
        }

        // Progress arrow
        int cookScale = tile.getCookProgressScaled(24);
        if (cookScale > 0) {
            this.drawTexturedModalRect(x + 79, y + 35, 176, 14, cookScale + 1, 17);
        }

        // Fluid tank fill
        int fluidScaled = tile.getFluidAmount() * 50 / Math.max(1, tile.getFluidCapacity());
        if (fluidScaled > 0) {
            drawRect(x + 57, y + 17 + 50 - fluidScaled, x + 57 + 12, y + 17 + 50, 0xFF2850DC);
        }

        // Gas tank fill
        int gasScaled = tile.getGasAmount() * 50 / Math.max(1, tile.getGasCapacity());
        if (gasScaled > 0) {
            drawRect(x + 113, y + 17 + 50 - gasScaled, x + 113 + 12, y + 17 + 50, 0xFFAAAAAA);
        }

    }
}
