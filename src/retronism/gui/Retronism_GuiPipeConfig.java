package retronism.gui;

import net.minecraft.src.*;
import retronism.api.*;
import org.lwjgl.opengl.GL11;

public class Retronism_GuiPipeConfig extends GuiScreen {
	private Retronism_ISideConfigurable tile;
	private EntityPlayer player;
	private int selectedType = -1;

	private static final int GUI_WIDTH = 176;
	private static final int GUI_HEIGHT = 166;
	private static final int FACE_SIZE = 36;
	private static final int FACE_GAP = 4;
	private static final int CROSS_START_Y = 34;

	// Cube-net layout: {col, row}
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

	private static final int[] MODE_FILL   = {0xFF8B8B8B, 0xFF3366CC, 0xFFCC6633, 0xFF33AA33};
	private static final int[] MODE_HILITE = {0xFFAAAAAA, 0xFF5599EE, 0xFFEE9966, 0xFF55CC55};
	private static final int[] MODE_SHADOW = {0xFF555555, 0xFF1A3366, 0xFF663319, 0xFF1A5519};
	private static final int[] TYPE_COLORS = {0xFFD4AA00, 0xFF3366FF, 0xFFAAAAAA, 0xFFFF8800};

	private static final int BTN_X = 112;
	private static final int BTN_Y = 4;
	private static final int BTN_W = 58;
	private static final int BTN_H = 14;

	public Retronism_GuiPipeConfig(EntityPlayer player, Retronism_ISideConfigurable tile) {
		this.player = player;
		this.tile = tile;
		this.selectedType = getFirstSupportedType();
	}

	private int getFirstSupportedType() {
		for (int t = 0; t < Retronism_SideConfig.TYPE_COUNT; t++) {
			if (tile.supportsType(t)) return t;
		}
		return 0;
	}

	private int getNextSupportedType(int current) {
		for (int i = 1; i <= Retronism_SideConfig.TYPE_COUNT; i++) {
			int next = (current + i) % Retronism_SideConfig.TYPE_COUNT;
			if (tile.supportsType(next)) return next;
		}
		return current;
	}

	private int countSupportedTypes() {
		int count = 0;
		for (int t = 0; t < Retronism_SideConfig.TYPE_COUNT; t++) {
			if (tile.supportsType(t)) count++;
		}
		return count;
	}

	private int cycleAllowed(int current, int[] allowed) {
		for (int i = 0; i < allowed.length; i++) {
			if (allowed[i] == current) return allowed[(i + 1) % allowed.length];
		}
		return allowed[0];
	}

	private int getCrossX(int guiX) {
		int crossW = 3 * FACE_SIZE + 2 * FACE_GAP;
		return guiX + (GUI_WIDTH - crossW) / 2;
	}

	public void drawScreen(int mouseX, int mouseY, float partialTick) {
		this.drawDefaultBackground();

		int guiX = (this.width - GUI_WIDTH) / 2;
		int guiY = (this.height - GUI_HEIGHT) / 2;

		// Panel background from texture
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		int textureID = this.mc.renderEngine.getTexture("/gui/retronism_side_config.png");
		this.mc.renderEngine.bindTexture(textureID);
		this.drawTexturedModalRect(guiX, guiY, 0, 0, GUI_WIDTH, GUI_HEIGHT);

		// Title
		this.fontRenderer.drawString("Pipe Config", guiX + 8, guiY + 6, 4210752);

		// Type selector button (only if >1 supported type)
		boolean showTypeBtn = countSupportedTypes() > 1;
		if (showTypeBtn) {
			int bx = guiX + BTN_X;
			int by = guiY + BTN_Y;
			int typeColor = TYPE_COLORS[selectedType];
			drawRect(bx, by, bx + BTN_W, by + BTN_H, 0xFF000000);
			drawRect(bx + 1, by + 1, bx + BTN_W - 1, by + BTN_H - 1, 0xFF555555);
			drawRect(bx + 1, by + 1, bx + BTN_W - 2, by + 2, 0xFFFFFFFF);
			drawRect(bx + 1, by + 1, bx + 2, by + BTN_H - 2, 0xFFFFFFFF);
			drawRect(bx + 2, by + 2, bx + BTN_W - 2, by + BTN_H - 2, 0xFF8B8B8B);
			drawRect(bx + 3, by + 3, bx + 11, by + BTN_H - 3, typeColor);
			this.fontRenderer.drawString(TYPE_NAMES[selectedType], bx + 13, by + 3, 0xFFFFFF);
		}

		int[] config = tile.getSideConfig();
		int crossX = getCrossX(guiX);
		int crossY = guiY + CROSS_START_Y;

		// Draw faces
		for (int side = 0; side < 6; side++) {
			int col = FACE_POS[side][0];
			int row = FACE_POS[side][1];
			int fx = crossX + col * (FACE_SIZE + FACE_GAP);
			int fy = crossY + row * (FACE_SIZE + FACE_GAP);

			int mode = Retronism_SideConfig.get(config, side, selectedType);
			int s = FACE_SIZE;

			// MC-style 3D raised button
			drawRect(fx, fy, fx + s, fy + s, 0xFF000000);
			drawRect(fx + 1, fy + 1, fx + s - 1, fy + s - 1, MODE_SHADOW[mode]);
			drawRect(fx + 1, fy + 1, fx + s - 2, fy + 2, MODE_HILITE[mode]);
			drawRect(fx + 1, fy + 1, fx + 2, fy + s - 2, MODE_HILITE[mode]);
			drawRect(fx + 2, fy + 2, fx + s - 2, fy + s - 2, MODE_FILL[mode]);

			// Side label centered
			String sideLabel = SIDE_LABELS[side];
			int labelW = this.fontRenderer.getStringWidth(sideLabel);
			this.fontRenderer.drawStringWithShadow(sideLabel, fx + (FACE_SIZE - labelW) / 2, fy + 6, 0xFFFFFF);

			// Mode label centered below
			String modeLabel = MODE_LABELS[mode];
			int modeW = this.fontRenderer.getStringWidth(modeLabel);
			int modeColor = (mode == Retronism_SideConfig.MODE_NONE) ? 0x999999 : 0xFFFFFF;
			this.fontRenderer.drawStringWithShadow(modeLabel, fx + (FACE_SIZE - modeW) / 2, fy + 20, modeColor);
		}

		// Tooltips
		for (int side = 0; side < 6; side++) {
			int col = FACE_POS[side][0];
			int row = FACE_POS[side][1];
			int fx = crossX + col * (FACE_SIZE + FACE_GAP);
			int fy = crossY + row * (FACE_SIZE + FACE_GAP);

			if (mouseX >= fx && mouseX < fx + FACE_SIZE && mouseY >= fy && mouseY < fy + FACE_SIZE) {
				int mode = Retronism_SideConfig.get(config, side, selectedType);
				String tip = Retronism_SideConfig.getSideName(side) + " "
					+ TYPE_NAMES[selectedType] + ": "
					+ Retronism_SideConfig.getModeName(mode);
				int tw = this.fontRenderer.getStringWidth(tip);
				int tx = mouseX + 8;
				int ty = mouseY - 12;
				drawRect(tx - 2, ty - 2, tx + tw + 2, ty + 10, 0xCC000000);
				this.fontRenderer.drawStringWithShadow(tip, tx, ty, 0xFFFFFF);
			}
		}

		// Type button tooltip
		if (showTypeBtn) {
			int bx = guiX + BTN_X;
			int by = guiY + BTN_Y;
			if (mouseX >= bx && mouseX < bx + BTN_W && mouseY >= by && mouseY < by + BTN_H) {
				String tip = "Click to change type";
				int tw = this.fontRenderer.getStringWidth(tip);
				int tx = mouseX + 8;
				int ty = mouseY - 12;
				drawRect(tx - 2, ty - 2, tx + tw + 2, ty + 10, 0xCC000000);
				this.fontRenderer.drawStringWithShadow(tip, tx, ty, 0xFFFFFF);
			}
		}

		super.drawScreen(mouseX, mouseY, partialTick);
	}

	protected void mouseClicked(int mouseX, int mouseY, int button) {
		int guiX = (this.width - GUI_WIDTH) / 2;
		int guiY = (this.height - GUI_HEIGHT) / 2;

		// Type button click
		if (countSupportedTypes() > 1) {
			int bx = guiX + BTN_X;
			int by = guiY + BTN_Y;
			if (mouseX >= bx && mouseX < bx + BTN_W && mouseY >= by && mouseY < by + BTN_H) {
				selectedType = getNextSupportedType(selectedType);
				return;
			}
		}

		// Face clicks
		int crossX = getCrossX(guiX);
		int crossY = guiY + CROSS_START_Y;

		for (int side = 0; side < 6; side++) {
			int col = FACE_POS[side][0];
			int row = FACE_POS[side][1];
			int fx = crossX + col * (FACE_SIZE + FACE_GAP);
			int fy = crossY + row * (FACE_SIZE + FACE_GAP);

			if (mouseX >= fx && mouseX < fx + FACE_SIZE && mouseY >= fy && mouseY < fy + FACE_SIZE) {
				int[] config = tile.getSideConfig();
				int oldMode = Retronism_SideConfig.get(config, side, selectedType);
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
				return;
			}
		}

		super.mouseClicked(mouseX, mouseY, button);
	}

	public boolean doesGuiPauseGame() {
		return false;
	}
}
