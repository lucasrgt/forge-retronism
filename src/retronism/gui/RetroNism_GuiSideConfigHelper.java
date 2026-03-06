package retronism.gui;

import net.minecraft.src.*;
import retronism.api.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class Retronism_GuiSideConfigHelper extends Gui {
	private Retronism_ISideConfigurable tile;
	private int machineBlockId;
	public boolean configMode = false;
	private static final RenderItem itemRenderer = new RenderItem();

	private static final int TAB_W = 30;
	private static final int TAB_H = 28;
	private static final int CELL_SIZE = 14;
	private static final int ROW_H = 22;
	private static final int START_Y = 24;
	private static final String[] SIDE_NAMES = {"Bottom", "Top", "North", "South", "West", "East"};
	private static final String[] TYPE_SHORT = {"E", "F", "G", "I"};

	public Retronism_GuiSideConfigHelper(Retronism_ISideConfigurable tile, int machineBlockId) {
		this.tile = tile;
		this.machineBlockId = machineBlockId;
	}

	public boolean isConfigMode() {
		return configMode;
	}

	// Draw the two tabs above the GUI. Always called.
	public void drawTabs(int guiLeft, int guiTop, FontRenderer font, RenderEngine renderEngine) {
		// --- Pass 1: Tab backgrounds (2D, z=300 overlay) ---
		GL11.glPushMatrix();
		GL11.glTranslatef(0, 0, 300.0F);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_DEPTH_TEST);

		drawTabBackground(guiLeft, guiTop, TAB_W, TAB_H, !configMode);
		drawTabBackground(guiLeft + TAB_W + 2, guiTop, TAB_W, TAB_H, configMode);

		// First tab inactive: extend left border down to merge with panel edge
		if (configMode) {
			drawRect(guiLeft, guiTop, guiLeft + 1, guiTop + 2, 0xFF000000);
			drawRect(guiLeft + 1, guiTop, guiLeft + 2, guiTop + 1, 0xFFFFFFFF);
		}

		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glPopMatrix();

		// --- Pass 2: Item icons ---
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glPushMatrix();
		GL11.glRotatef(120.0F, 1.0F, 0.0F, 0.0F);
		RenderHelper.enableStandardItemLighting();
		GL11.glPopMatrix();
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		GL11.glEnable(GL11.GL_DEPTH_TEST);

		GL11.glPushMatrix();
		GL11.glTranslatef(0, 0, 300.0F);

		int top = guiTop - TAB_H;
		int iconY = top + (TAB_H - 16) / 2 + 1;
		itemRenderer.renderItemIntoGUI(font, renderEngine,
			new ItemStack(machineBlockId, 1, 0),
			guiLeft + (TAB_W - 16) / 2, iconY);
		itemRenderer.renderItemIntoGUI(font, renderEngine,
			new ItemStack(mod_Retronism.wrench, 1, 0),
			guiLeft + TAB_W + 2 + (TAB_W - 16) / 2, iconY);

		GL11.glPopMatrix();
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		RenderHelper.disableStandardItemLighting();
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private void drawTabBackground(int x, int y, int w, int h, boolean active) {
		int top = y - h;
		int bot = active ? (y + 3) : y;
		int bg = active ? 0xFFC6C6C6 : 0xFF8B8B8B;

		// 1. Fill entire interior with bg first (no gaps)
		drawRect(x + 1, top + 1, x + w - 3, top + 2, bg);
		drawRect(x + 1, top + 2, x + w - 1, bot, bg);

		// 2. Black outer border (on top of bg)
		// Top edge + left corner (1-step) + right corner (2-step staircase)
		drawRect(x + 2, top, x + w - 3, top + 1, 0xFF000000);
		drawRect(x + 1, top + 1, x + 2, top + 2, 0xFF000000);
		drawRect(x + w - 3, top + 1, x + w - 2, top + 2, 0xFF000000);
		drawRect(x + w - 2, top + 2, x + w - 1, top + 3, 0xFF000000);
		drawRect(x, top + 2, x + 1, bot, 0xFF000000);
		drawRect(x + w - 1, top + 3, x + w, bot, 0xFF000000);

		// 3. White highlight (2px thick, inner top + inner left)
		drawRect(x + 2, top + 1, x + w - 3, top + 3, 0xFFFFFFFF);
		drawRect(x + 1, top + 2, x + 3, bot, 0xFFFFFFFF);

		// 4. Dark shadow (2px thick, inner right — 1px bg gap from highlight)
		drawRect(x + w - 3, top + 3, x + w - 1, bot, 0xFF555555);

		// 5. Round inner corner of highlight L-shape (1px diagonal)
		drawRect(x + 3, top + 3, x + 4, top + 4, 0xFFFFFFFF);

		// Active: connection area merges tab into panel
		if (active) {
			drawRect(x + 1, y, x + w - 1, y + 3, bg);
			drawRect(x, y, x + 1, y + 3, 0xFF000000);
			drawRect(x + w - 1, y, x + w, y + 1, 0xFF000000);
			drawRect(x + w - 1, y + 1, x + w, y + 3, 0xFFFFFFFF);
			drawRect(x + 1, y, x + 3, y + 3, 0xFFFFFFFF);
			drawRect(x + w - 3, y, x + w - 1, y + 3, 0xFF555555);
			drawRect(x + 3, y, x + w - 3, y + 3, bg);
			drawRect(x + w - 2, y + 2, x + w - 1, y + 3, 0xFFFFFFFF);
			// Cover stray white pixel from GUI panel border below left highlight
			drawRect(x + 3, y + 3, x + 5, y + 4, bg);
			// Extend left border down to merge with panel edge
			drawRect(x, y + 3, x + 1, y + 5, 0xFF000000);
			drawRect(x + 1, y + 3, x + 2, y + 4, 0xFFFFFFFF);
		}
	}

	// Draw the full config overlay (replaces entire GUI content). Only called when configMode=true.
	public void drawConfigOverlay(int guiLeft, int guiTop, int xSize, int ySize, FontRenderer font, int mouseX, int mouseY, RenderEngine renderEngine) {
		// Push GL state to render on top of everything
		GL11.glPushMatrix();
		GL11.glTranslatef(0, 0, 300.0F);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		// Draw GUI texture background (176x166 standard panel)
		int textureID = renderEngine.getTexture("/gui/retronism_side_config.png");
		renderEngine.bindTexture(textureID);
		this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, 176, 166);

		// Title
		font.drawString("Side Configuration", guiLeft + 8, guiTop + 6, 4210752);

		int[] config = tile.getSideConfig();

		for (int side = 0; side < 6; side++) {
			int rowY = guiTop + START_Y + side * ROW_H;

			// Side name
			font.drawString(SIDE_NAMES[side], guiLeft + 10, rowY + 4, 4210752);

			// Type cells
			int cellX = guiLeft + 60;
			for (int type = 0; type < Retronism_SideConfig.TYPE_COUNT; type++) {
				if (!tile.supportsType(type)) continue;
				int mode = Retronism_SideConfig.get(config, side, type);
				int color = Retronism_SideConfig.getColor(type, mode);

				// Cell border + fill
				drawRect(cellX, rowY + 1, cellX + CELL_SIZE, rowY + 1 + CELL_SIZE, 0xFF373737);
				drawRect(cellX + 1, rowY + 2, cellX + CELL_SIZE - 1, rowY + CELL_SIZE, color);

				// Type letter centered
				String label = TYPE_SHORT[type];
				int lw = font.getStringWidth(label);
				font.drawString(label, cellX + (CELL_SIZE - lw) / 2, rowY + 4, 0xFFFFFF);

				// Mode name next to cell
				String modeName = Retronism_SideConfig.getModeName(mode);
				font.drawString(modeName, cellX + CELL_SIZE + 3, rowY + 4, 4210752);

				cellX += CELL_SIZE + font.getStringWidth(modeName) + 8;
			}
		}

		// Tooltip on hover
		for (int side = 0; side < 6; side++) {
			int rowY = guiTop + START_Y + side * ROW_H;
			int cellX = guiLeft + 60;
			for (int type = 0; type < Retronism_SideConfig.TYPE_COUNT; type++) {
				if (!tile.supportsType(type)) continue;
				int mode = Retronism_SideConfig.get(config, side, type);
				if (mouseX >= cellX && mouseX < cellX + CELL_SIZE && mouseY >= rowY + 1 && mouseY < rowY + 1 + CELL_SIZE) {
					String tip = "Click to cycle: " + Retronism_SideConfig.getModeName(mode)
						+ " -> " + Retronism_SideConfig.getModeName(Retronism_SideConfig.cycleMode(mode));
					int tw = font.getStringWidth(tip);
					int tx = mouseX + 8;
					int ty = mouseY - 12;
					drawRect(tx - 2, ty - 2, tx + tw + 2, ty + 10, 0xCC000000);
					font.drawStringWithShadow(tip, tx, ty, 0xFFFFFF);
				}
				String modeName = Retronism_SideConfig.getModeName(mode);
				cellX += CELL_SIZE + font.getStringWidth(modeName) + 8;
			}
		}

		// Restore GL state
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glPopMatrix();
	}

	// Handle clicks. Returns true if consumed.
	public boolean handleClick(int mouseX, int mouseY, int guiLeft, int guiTop, int xSize, int ySize, FontRenderer font) {
		// Tab 1: Main
		int t1x = guiLeft;
		int tabTop = guiTop - TAB_H;
		if (mouseX >= t1x && mouseX < t1x + TAB_W && mouseY >= tabTop && mouseY < guiTop + 1) {
			configMode = false;
			return true;
		}

		// Tab 2: Config
		int t2x = guiLeft + TAB_W + 2;
		if (mouseX >= t2x && mouseX < t2x + TAB_W && mouseY >= tabTop && mouseY < guiTop + 1) {
			configMode = true;
			return true;
		}

		// Config cell clicks
		if (!configMode) return false;

		int[] config = tile.getSideConfig();

		for (int side = 0; side < 6; side++) {
			int rowY = guiTop + START_Y + side * ROW_H;
			int cellX = guiLeft + 60;
			for (int type = 0; type < Retronism_SideConfig.TYPE_COUNT; type++) {
				if (!tile.supportsType(type)) continue;
				if (mouseX >= cellX && mouseX < cellX + CELL_SIZE && mouseY >= rowY + 1 && mouseY < rowY + 1 + CELL_SIZE) {
					int oldMode = Retronism_SideConfig.get(config, side, type);
					int newMode = Retronism_SideConfig.cycleMode(oldMode);
					tile.setSideMode(side, type, newMode);
					if (tile instanceof TileEntity) {
						TileEntity te = (TileEntity) tile;
						te.worldObj.markBlockNeedsUpdate(te.xCoord, te.yCoord, te.zCoord);
					}
					return true;
				}
				int mode = Retronism_SideConfig.get(config, side, type);
				String modeName = Retronism_SideConfig.getModeName(mode);
				cellX += CELL_SIZE + font.getStringWidth(modeName) + 8;
			}
		}

		// Consume all clicks inside the GUI area when in config mode
		if (mouseX >= guiLeft && mouseX < guiLeft + xSize && mouseY >= guiTop && mouseY < guiTop + ySize) {
			return true;
		}

		return false;
	}
}
