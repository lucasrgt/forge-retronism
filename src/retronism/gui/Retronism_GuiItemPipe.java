package retronism.gui;

import net.minecraft.src.*;
import retronism.*;
import retronism.tile.*;
import retronism.container.*;
import aero.machineapi.*;
import org.lwjgl.opengl.GL11;

public class Retronism_GuiItemPipe extends GuiContainer {
	private Retronism_TileItemPipe pipe;
	private Retronism_GuiSideConfigHelper sideConfigHelper;
	private int mouseX;
	private int mouseY;

	// Dynamic UI element positions (relative to guiLeft/guiTop)
	private static final int BTN_X = 66;
	private static final int BTN_Y = 18;
	private static final int BTN_W = 58;
	private static final int BTN_H = 14;

	private static final int PRIO_X = 66;
	private static final int PRIO_Y = 36;
	private static final int PRIO_BOX_X = 118;

	private static final String[] SIDE_LABELS = {"Bot", "Top", "N", "S", "W", "E"};

	public Retronism_GuiItemPipe(InventoryPlayer playerInv, Retronism_TileItemPipe pipe) {
		super(new Retronism_ContainerItemPipe(playerInv, pipe));
		this.pipe = pipe;
		this.sideConfigHelper = new Retronism_GuiSideConfigHelper(pipe, Retronism_Registry.itemPipeBlock.blockID);
	}

	protected void drawGuiContainerForegroundLayer() {
		this.fontRenderer.drawString("Item Pipe", 8, 6, 4210752);
		this.fontRenderer.drawString("Inventory", 8, this.ySize - 96 + 2, 4210752);

		// Whitelist/Blacklist button
		boolean wl = pipe.isWhitelist();
		int btnColor = wl ? 0xFF33AA33 : 0xFFCC3333;
		String btnLabel = wl ? "Whitelist" : "Blacklist";
		drawRect(BTN_X, BTN_Y, BTN_X + BTN_W, BTN_Y + BTN_H, 0xFF000000);
		drawRect(BTN_X + 1, BTN_Y + 1, BTN_X + BTN_W - 1, BTN_Y + BTN_H - 1, 0xFF555555);
		drawRect(BTN_X + 1, BTN_Y + 1, BTN_X + BTN_W - 2, BTN_Y + 2, 0xFFFFFFFF);
		drawRect(BTN_X + 1, BTN_Y + 1, BTN_X + 2, BTN_Y + BTN_H - 2, 0xFFFFFFFF);
		drawRect(BTN_X + 2, BTN_Y + 2, BTN_X + BTN_W - 2, BTN_Y + BTN_H - 2, btnColor);
		int lw = this.fontRenderer.getStringWidth(btnLabel);
		this.fontRenderer.drawStringWithShadow(btnLabel, BTN_X + (BTN_W - lw) / 2, BTN_Y + 3, 0xFFFFFF);

		// Global priority
		this.fontRenderer.drawString("Priority:", PRIO_X, PRIO_Y + 2, 4210752);
		drawPriorityBox(PRIO_BOX_X, PRIO_Y, pipe.getPriority());

		// Side priorities
		this.fontRenderer.drawString("Side Priority", PRIO_X, PRIO_Y + 16, 4210752);
		for (int side = 0; side < 6; side++) {
			int col = side % 3;
			int row = side / 3;
			int lx = PRIO_X + col * 36;
			int ly = PRIO_Y + 28 + row * 14;
			this.fontRenderer.drawString(SIDE_LABELS[side], lx, ly + 2, 4210752);
			drawPriorityBox(lx + 16, ly, pipe.getSidePriority(side));
		}

		// Tooltips
		int relX = this.mouseX - (this.width - this.xSize) / 2;
		int relY = this.mouseY - (this.height - this.ySize) / 2;

		// Whitelist/Blacklist tooltip
		if (relX >= BTN_X && relX < BTN_X + BTN_W && relY >= BTN_Y && relY < BTN_Y + BTN_H) {
			String tip = wl ? "Only filtered items pass" : "Filtered items are blocked";
			int tw = this.fontRenderer.getStringWidth(tip);
			int tx = relX + 8;
			int ty = relY - 12;
			drawRect(tx - 2, ty - 2, tx + tw + 2, ty + 10, 0xCC000000);
			this.fontRenderer.drawStringWithShadow(tip, tx, ty, 0xFFFFFF);
		}

		// Ghost slot tooltips
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int i = row * 3 + col;
				int sx = 7 + col * 18;
				int sy = 17 + row * 18;
				if (relX >= sx && relX < sx + 18 && relY >= sy && relY < sy + 18) {
					ItemStack filterItem = pipe.getFilterSlot(i);
					if (filterItem != null) {
						String name = filterItem.getItemName();
						int tw = this.fontRenderer.getStringWidth(name);
						int tx = relX + 8;
						int ty = relY - 12;
						drawRect(tx - 2, ty - 2, tx + tw + 2, ty + 10, 0xCC000000);
						this.fontRenderer.drawStringWithShadow(name, tx, ty, 0xFFFFFF);
					}
				}
			}
		}
	}

	private void drawPriorityBox(int x, int y, int value) {
		int boxW = 14;
		int boxH = 12;
		drawRect(x, y, x + boxW, y + boxH, 0xFF000000);
		drawRect(x + 1, y + 1, x + boxW - 1, y + boxH - 1, 0xFF555555);
		drawRect(x + 1, y + 1, x + boxW - 2, y + 2, 0xFFFFFFFF);
		drawRect(x + 1, y + 1, x + 2, y + boxH - 2, 0xFFFFFFFF);
		drawRect(x + 2, y + 2, x + boxW - 2, y + boxH - 2, 0xFF8B8B8B);
		String num = String.valueOf(value);
		int nw = this.fontRenderer.getStringWidth(num);
		this.fontRenderer.drawStringWithShadow(num, x + (boxW - nw) / 2, y + 2, 0xFFFFFF);
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

		int relX = mouseX - guiLeft;
		int relY = mouseY - guiTop;

		// Whitelist/Blacklist button click
		if (relX >= BTN_X && relX < BTN_X + BTN_W && relY >= BTN_Y && relY < BTN_Y + BTN_H) {
			pipe.setWhitelist(!pipe.isWhitelist());
			return;
		}

		// Global priority click
		if (relX >= PRIO_BOX_X && relX < PRIO_BOX_X + 14 && relY >= PRIO_Y && relY < PRIO_Y + 12) {
			if (button == 0) {
				pipe.setPriority((pipe.getPriority() + 1) % 10);
			} else {
				pipe.setPriority((pipe.getPriority() + 9) % 10);
			}
			return;
		}

		// Side priority clicks
		for (int side = 0; side < 6; side++) {
			int col = side % 3;
			int row = side / 3;
			int numX = PRIO_X + col * 36 + 16;
			int numY = PRIO_Y + 28 + row * 14;
			if (relX >= numX && relX < numX + 14 && relY >= numY && relY < numY + 12) {
				if (button == 0) {
					pipe.setSidePriority(side, (pipe.getSidePriority(side) + 1) % 10);
				} else {
					pipe.setSidePriority(side, (pipe.getSidePriority(side) + 9) % 10);
				}
				return;
			}
		}

		super.mouseClicked(mouseX, mouseY, button);
	}

	protected void drawGuiContainerBackgroundLayer(float partialTick) {
		int textureID = this.mc.renderEngine.getTexture("/gui/retronism_item_pipe.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.renderEngine.bindTexture(textureID);
		int x = (this.width - this.xSize) / 2;
		int y = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
	}
}
