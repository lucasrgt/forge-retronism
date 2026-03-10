package retronism.gui;

import net.minecraft.src.*;
import retronism.*;
import aero.machineapi.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class Retronism_GuiSideConfigHelper extends Gui {
	private Aero_ISideConfigurable tile;
	private int machineBlockId;
	public boolean configMode = false;
	private int selectedType = -1;
	private static final RenderItem itemRenderer = new RenderItem();

	private static final int TAB_W = 30;
	private static final int TAB_H = 28;

	// Cross layout constants
	private static final int FACE_SIZE = 36;
	private static final int FACE_GAP = 4;
	// Vertically centered: title ~18px, available = 166-18 = 148, cross = 3*36+2*4 = 116, margin = (148-116)/2 = 16
	private static final int CROSS_START_Y = 34;

	// Cube-net layout: {col, row} for each side
	//        [Top]
	// [West] [North] [East]
	// [South] [Bot]
	private static final int[][] FACE_POS = {
		{1, 2}, // BOTTOM
		{1, 0}, // TOP
		{1, 1}, // NORTH
		{0, 2}, // SOUTH
		{0, 1}, // WEST
		{2, 1}, // EAST
	};

	private static final String[] SIDE_LABELS = {"Bot", "Top", "N", "S", "W", "E"};
	private static final String[] TYPE_NAMES = {"Energy", "Fluid", "Gas", "Item"};
	private static final String[] MODE_LABELS = {"Off", "Input", "Output", "I/O"};

	// Standardized mode colors (same for all types): fill, highlight (top-left), shadow (bottom-right)
	private static final int[] MODE_FILL   = {0xFF8B8B8B, 0xFF3366CC, 0xFFCC6633, 0xFF33AA33};
	private static final int[] MODE_HILITE = {0xFFAAAAAA, 0xFF5599EE, 0xFFEE9966, 0xFF55CC55};
	private static final int[] MODE_SHADOW = {0xFF555555, 0xFF1A3366, 0xFF663319, 0xFF1A5519};
	// Type indicator colors (for the button only)
	private static final int[] TYPE_COLORS = {0xFFD4AA00, 0xFF3366FF, 0xFFAAAAAA, 0xFFFF8800};

	// Type button position (relative to guiLeft/guiTop)
	private static final int BTN_X = 112;
	private static final int BTN_Y = 4;
	private static final int BTN_W = 58;
	private static final int BTN_H = 14;

	public Retronism_GuiSideConfigHelper(Aero_ISideConfigurable tile, int machineBlockId) {
		this.tile = tile;
		this.machineBlockId = machineBlockId;
		this.selectedType = getFirstSupportedType();
	}

	private int getFirstSupportedType() {
		for (int t = 0; t < Aero_SideConfig.TYPE_COUNT; t++) {
			if (tile.supportsType(t)) return t;
		}
		return 0;
	}

	private int cycleAllowed(int current, int[] allowed) {
		for (int i = 0; i < allowed.length; i++) {
			if (allowed[i] == current) {
				return allowed[(i + 1) % allowed.length];
			}
		}
		return allowed[0];
	}

	private int getNextSupportedType(int current) {
		for (int i = 1; i <= Aero_SideConfig.TYPE_COUNT; i++) {
			int next = (current + i) % Aero_SideConfig.TYPE_COUNT;
			if (tile.supportsType(next)) return next;
		}
		return current;
	}

	public boolean isConfigMode() {
		return configMode;
	}

	private int getCrossX(int guiLeft) {
		int crossW = 3 * FACE_SIZE + 2 * FACE_GAP;
		return guiLeft + (176 - crossW) / 2;
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
			new ItemStack(Retronism_Registry.wrench, 1, 0),
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
		drawRect(x + 2, top, x + w - 3, top + 1, 0xFF000000);
		drawRect(x + 1, top + 1, x + 2, top + 2, 0xFF000000);
		drawRect(x + w - 3, top + 1, x + w - 2, top + 2, 0xFF000000);
		drawRect(x + w - 2, top + 2, x + w - 1, top + 3, 0xFF000000);
		drawRect(x, top + 2, x + 1, bot, 0xFF000000);
		drawRect(x + w - 1, top + 3, x + w, bot, 0xFF000000);

		// 3. White highlight (2px thick, inner top + inner left)
		drawRect(x + 2, top + 1, x + w - 3, top + 3, 0xFFFFFFFF);
		drawRect(x + 1, top + 2, x + 3, bot, 0xFFFFFFFF);

		// 4. Dark shadow (2px thick, inner right)
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
			drawRect(x + 3, y + 3, x + 5, y + 4, bg);
			drawRect(x, y + 3, x + 1, y + 5, 0xFF000000);
			drawRect(x + 1, y + 3, x + 2, y + 4, 0xFFFFFFFF);
		}
	}

	// Draw the full config overlay. Only called when configMode=true.
	public void drawConfigOverlay(int guiLeft, int guiTop, int xSize, int ySize, FontRenderer font, int mouseX, int mouseY, RenderEngine renderEngine) {
		GL11.glPushMatrix();
		GL11.glTranslatef(0, 0, 300.0F);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		// Draw panel background from texture (rounded corners like furnace)
		int textureID = renderEngine.getTexture("/gui/retronism_side_config.png");
		renderEngine.bindTexture(textureID);
		this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, 176, 166);

		// Title
		font.drawString("Configuration", guiLeft + 8, guiTop + 6, 4210752);

		// Type selector button
		int bx = guiLeft + BTN_X;
		int by = guiTop + BTN_Y;
		int typeColor = TYPE_COLORS[selectedType];
		// Button border (3D MC style)
		drawRect(bx, by, bx + BTN_W, by + BTN_H, 0xFF000000);
		drawRect(bx + 1, by + 1, bx + BTN_W - 1, by + BTN_H - 1, 0xFF555555);
		drawRect(bx + 1, by + 1, bx + BTN_W - 2, by + 2, 0xFFFFFFFF);
		drawRect(bx + 1, by + 1, bx + 2, by + BTN_H - 2, 0xFFFFFFFF);
		drawRect(bx + 2, by + 2, bx + BTN_W - 2, by + BTN_H - 2, 0xFF8B8B8B);
		// Color indicator square
		drawRect(bx + 3, by + 3, bx + 11, by + BTN_H - 3, typeColor);
		// Type name
		font.drawString(TYPE_NAMES[selectedType], bx + 13, by + 3, 0xFFFFFF);

		int[] config = tile.getSideConfig();
		int crossX = getCrossX(guiLeft);
		int crossY = guiTop + CROSS_START_Y;

		// Draw faces
		for (int side = 0; side < 6; side++) {
			int col = FACE_POS[side][0];
			int row = FACE_POS[side][1];
			int fx = crossX + col * (FACE_SIZE + FACE_GAP);
			int fy = crossY + row * (FACE_SIZE + FACE_GAP);

			int mode = Aero_SideConfig.get(config, side, selectedType);
			int s = FACE_SIZE;

			// MC-style 3D raised button
			drawRect(fx, fy, fx + s, fy + s, 0xFF000000);             // outer border
			drawRect(fx + 1, fy + 1, fx + s - 1, fy + s - 1, MODE_SHADOW[mode]); // shadow base
			drawRect(fx + 1, fy + 1, fx + s - 2, fy + 2, MODE_HILITE[mode]);     // top highlight
			drawRect(fx + 1, fy + 1, fx + 2, fy + s - 2, MODE_HILITE[mode]);     // left highlight
			drawRect(fx + 2, fy + 2, fx + s - 2, fy + s - 2, MODE_FILL[mode]);   // fill

			// Side label centered
			String sideLabel = SIDE_LABELS[side];
			int labelW = font.getStringWidth(sideLabel);
			font.drawStringWithShadow(sideLabel, fx + (FACE_SIZE - labelW) / 2, fy + 6, 0xFFFFFF);

			// Mode label centered below
			String modeLabel = MODE_LABELS[mode];
			int modeW = font.getStringWidth(modeLabel);
			int modeColor = (mode == Aero_SideConfig.MODE_NONE) ? 0x999999 : 0xFFFFFF;
			font.drawStringWithShadow(modeLabel, fx + (FACE_SIZE - modeW) / 2, fy + 20, modeColor);
		}

		// Tooltip on hover
		for (int side = 0; side < 6; side++) {
			int col = FACE_POS[side][0];
			int row = FACE_POS[side][1];
			int fx = crossX + col * (FACE_SIZE + FACE_GAP);
			int fy = crossY + row * (FACE_SIZE + FACE_GAP);

			if (mouseX >= fx && mouseX < fx + FACE_SIZE && mouseY >= fy && mouseY < fy + FACE_SIZE) {
				int mode = Aero_SideConfig.get(config, side, selectedType);
				String tip = Aero_SideConfig.getSideName(side) + " "
					+ TYPE_NAMES[selectedType] + ": "
					+ Aero_SideConfig.getModeName(mode);
				int tw = font.getStringWidth(tip);
				int tx = mouseX + 8;
				int ty = mouseY - 12;
				drawRect(tx - 2, ty - 2, tx + tw + 2, ty + 10, 0xCC000000);
				font.drawStringWithShadow(tip, tx, ty, 0xFFFFFF);
			}
		}

		// Tooltip on type button hover
		if (mouseX >= bx && mouseX < bx + BTN_W && mouseY >= by && mouseY < by + BTN_H) {
			String tip = "Click to change type";
			int tw = font.getStringWidth(tip);
			int tx = mouseX + 8;
			int ty = mouseY - 12;
			drawRect(tx - 2, ty - 2, tx + tw + 2, ty + 10, 0xCC000000);
			font.drawStringWithShadow(tip, tx, ty, 0xFFFFFF);
		}

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

		if (!configMode) return false;

		// Type button click
		int bx = guiLeft + BTN_X;
		int by = guiTop + BTN_Y;
		if (mouseX >= bx && mouseX < bx + BTN_W && mouseY >= by && mouseY < by + BTN_H) {
			selectedType = getNextSupportedType(selectedType);
			return true;
		}

		// Face clicks
		int crossX = getCrossX(guiLeft);
		int crossY = guiTop + CROSS_START_Y;

		for (int side = 0; side < 6; side++) {
			int col = FACE_POS[side][0];
			int row = FACE_POS[side][1];
			int fx = crossX + col * (FACE_SIZE + FACE_GAP);
			int fy = crossY + row * (FACE_SIZE + FACE_GAP);

			if (mouseX >= fx && mouseX < fx + FACE_SIZE && mouseY >= fy && mouseY < fy + FACE_SIZE) {
				int[] config = tile.getSideConfig();
				int oldMode = Aero_SideConfig.get(config, side, selectedType);
				int[] allowed = tile.getAllowedModes(selectedType);
				int newMode = cycleAllowed(oldMode, allowed);
				tile.setSideMode(side, selectedType, newMode);
				if (tile instanceof TileEntity) {
					TileEntity te = (TileEntity) tile;
					te.worldObj.markBlockNeedsUpdate(te.xCoord, te.yCoord, te.zCoord);
					for (int[] d : new int[][]{{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}}) {
						te.worldObj.markBlockNeedsUpdate(te.xCoord+d[0], te.yCoord+d[1], te.zCoord+d[2]);
					}
				}
				return true;
			}
		}

		// Consume all clicks inside the GUI area when in config mode
		if (mouseX >= guiLeft && mouseX < guiLeft + xSize && mouseY >= guiTop && mouseY < guiTop + ySize) {
			return true;
		}

		return false;
	}
}
