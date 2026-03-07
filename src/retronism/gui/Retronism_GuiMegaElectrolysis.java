package retronism.gui;

import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;
import retronism.tile.Retronism_TileMegaElectrolysis;
import retronism.container.Retronism_ContainerMegaElectrolysis;

public class Retronism_GuiMegaElectrolysis extends GuiContainer {

    private Retronism_TileMegaElectrolysis tile;
    private int textureID;

    public Retronism_GuiMegaElectrolysis(InventoryPlayer playerInv, Retronism_TileMegaElectrolysis tile) {
        super(new Retronism_ContainerMegaElectrolysis(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    protected void drawGuiContainerForegroundLayer() {
        fontRenderer.drawString("MegaElectrolysis", (xSize - fontRenderer.getStringWidth("MegaElectrolysis")) / 2, 6, 4210752);
        fontRenderer.drawString("Inventory", 8, ySize - 96 + 2, 4210752);

        if (!tile.isFormed) {
            fontRenderer.drawString("Structure incomplete!", 8, 20, 0xFF4444);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        textureID = this.mc.renderEngine.getTexture("/gui/retronism_megaelectrolysis.png");
        this.mc.renderEngine.bindTexture(textureID);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);

        // Energy bar fill
        int barX = x + 162, barY = y + 17, barW = 6, barH = 52;
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
            this.drawTexturedModalRect(x + 76, y + 34, 176, 14, cookScale + 1, 17);
        }

        // Fluid tank fill
        int fluidScaled = tile.getFluidAmount() * 52 / Math.max(1, tile.getFluidCapacity());
        if (fluidScaled > 0) {
            drawRect(x + 31, y + 17 + 52 - fluidScaled, x + 31 + 14, y + 17 + 52, 0xFF2850DC);
        }

        // Gas tank fill
        int gasScaled = tile.getGasAmount() * 52 / Math.max(1, tile.getGasCapacity());
        if (gasScaled > 0) {
            drawRect(x + 117, y + 17 + 52 - gasScaled, x + 117 + 14, y + 17 + 52, 0xFFAAAAAA);
        }

    }
}
