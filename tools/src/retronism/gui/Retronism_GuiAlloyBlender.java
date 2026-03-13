package retronism.gui;

import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;
import retronism.tile.Retronism_TileAlloyBlender;
import retronism.container.Retronism_ContainerAlloyBlender;

public class Retronism_GuiAlloyBlender extends GuiContainer {

    private Retronism_TileAlloyBlender tile;
    private int textureID;
    private int mouseX;
    private int mouseY;

    public Retronism_GuiAlloyBlender(InventoryPlayer playerInv, Retronism_TileAlloyBlender tile) {
        super(new Retronism_ContainerAlloyBlender(playerInv, tile));
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
        fontRenderer.drawString("AlloyBlender", (xSize - fontRenderer.getStringWidth("AlloyBlender")) / 2, 6, 4210752);
        fontRenderer.drawString("Inventory", 7, ySize - 96 + 2, 4210752);

        if (!tile.isFormed) {
            fontRenderer.drawString("Structure incomplete!", 8, 20, 0xFF4444);
            return;
        }

        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;
        int relMouseX = this.mouseX - guiLeft;
        int relMouseY = this.mouseY - guiTop;

        String tooltip = null;


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
        textureID = this.mc.renderEngine.getTexture("/gui/retronism_alloyblender.png");
        this.mc.renderEngine.bindTexture(textureID);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);

        // Progress arrow
        int cookScale = tile.getCookProgressScaled(24);
        if (cookScale > 0) {
            this.drawTexturedModalRect(x + 99, y + 35, 176, 14, cookScale + 1, 17);
        }

    }
}
