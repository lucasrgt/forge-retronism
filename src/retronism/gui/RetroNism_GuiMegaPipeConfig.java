package retronism.gui;

import net.minecraft.src.*;
import aero.machineapi.*;
import retronism.tile.*;
import org.lwjgl.opengl.GL11;

public class Retronism_GuiMegaPipeConfig extends GuiScreen {
	private Retronism_TileMegaPipe tile;
	private EntityPlayer player;

	private static final int GUI_WIDTH = 176;
	private static final int GUI_HEIGHT = 140;
	private static final int FACE_SIZE = 40;
	private static final int CELL_SIZE = 16;
	private static final int GAP = 4;

	// Cross layout offsets (col, row) for each side
	// Layout:       [Top]
	//  [West] [North] [East]
	//       [Bottom]
	//       [South]
	private static final int[][] FACE_POS = {
		{1, 3}, // BOTTOM
		{1, 0}, // TOP
		{1, 1}, // NORTH (front/center)
		{1, 2}, // SOUTH
		{0, 1}, // WEST
		{2, 1}, // EAST
	};

	private static final String[] SIDE_LABELS = {"Bottom", "Top", "North", "South", "West", "East"};
	private static final String[] TYPE_LABELS = {"E", "F", "G", "I"};

	public Retronism_GuiMegaPipeConfig(EntityPlayer player, Retronism_TileMegaPipe tile) {
		this.player = player;
		this.tile = tile;
	}

	private int cycleAllowed(int current, int[] allowed) {
		for (int i = 0; i < allowed.length; i++) {
			if (allowed[i] == current) return allowed[(i + 1) % allowed.length];
		}
		return allowed[0];
	}

	public void drawScreen(int mouseX, int mouseY, float partialTick) {
		this.drawDefaultBackground();

		int guiX = (this.width - GUI_WIDTH) / 2;
		int guiY = (this.height - GUI_HEIGHT) / 2;

		// Background panel
		drawRect(guiX - 4, guiY - 4, guiX + GUI_WIDTH + 4, guiY + GUI_HEIGHT + 4, 0xFF000000);
		drawRect(guiX - 3, guiY - 3, guiX + GUI_WIDTH + 3, guiY + GUI_HEIGHT + 3, 0xFFC6C6C6);

		// Title
		this.drawCenteredString(this.fontRenderer, "Mega Pipe Config", guiX + GUI_WIDTH / 2, guiY + 2, 0xFFFFFF);

		int startY = guiY + 16;

		int[] config = tile.getSideConfig();

		// Draw each face
		for (int side = 0; side < 6; side++) {
			int col = FACE_POS[side][0];
			int row = FACE_POS[side][1];
			int fx = guiX + 28 + col * (FACE_SIZE + GAP);
			int fy = startY + row * (FACE_SIZE + GAP);

			// Face background
			drawRect(fx, fy, fx + FACE_SIZE, fy + FACE_SIZE, 0xFF333333);
			drawRect(fx + 1, fy + 1, fx + FACE_SIZE - 1, fy + FACE_SIZE - 1, 0xFF555555);

			// Side label
			this.fontRenderer.drawString(SIDE_LABELS[side], fx + 2, fy + 2, 0xFFFFFF);

			// 4 type cells (2x2 grid)
			for (int type = 0; type < 4; type++) {
				int cx = fx + 4 + (type % 2) * (CELL_SIZE + 2);
				int cy = fy + 14 + (type / 2) * (CELL_SIZE + 2);
				int mode = Aero_SideConfig.get(config, side, type);
				int color = Aero_SideConfig.getColor(type, mode);

				// Cell background (color)
				drawRect(cx, cy, cx + CELL_SIZE, cy + CELL_SIZE, 0xFF000000);
				drawRect(cx + 1, cy + 1, cx + CELL_SIZE - 1, cy + CELL_SIZE - 1, color);

				// Type label centered
				String label = TYPE_LABELS[type];
				int labelW = this.fontRenderer.getStringWidth(label);
				this.fontRenderer.drawString(label, cx + (CELL_SIZE - labelW) / 2, cy + 4, 0xFFFFFF);
			}
		}

		// Tooltip
		for (int side = 0; side < 6; side++) {
			int col = FACE_POS[side][0];
			int row = FACE_POS[side][1];
			int fx = guiX + 28 + col * (FACE_SIZE + GAP);
			int fy = startY + row * (FACE_SIZE + GAP);

			for (int type = 0; type < 4; type++) {
				int cx = fx + 4 + (type % 2) * (CELL_SIZE + 2);
				int cy = fy + 14 + (type / 2) * (CELL_SIZE + 2);
				if (mouseX >= cx && mouseX < cx + CELL_SIZE && mouseY >= cy && mouseY < cy + CELL_SIZE) {
					int mode = Aero_SideConfig.get(config, side, type);
					String tip = SIDE_LABELS[side] + " " + Aero_SideConfig.getTypeName(type)
						+ ": " + Aero_SideConfig.getModeName(mode);
					int tw = this.fontRenderer.getStringWidth(tip);
					int tx = mouseX + 8;
					int ty = mouseY - 12;
					drawRect(tx - 2, ty - 2, tx + tw + 2, ty + 10, 0xCC000000);
					this.fontRenderer.drawStringWithShadow(tip, tx, ty, 0xFFFFFF);
				}
			}
		}

		super.drawScreen(mouseX, mouseY, partialTick);
	}

	protected void mouseClicked(int mouseX, int mouseY, int button) {
		int guiX = (this.width - GUI_WIDTH) / 2;
		int startY = (this.height - GUI_HEIGHT) / 2 + 16;

		for (int side = 0; side < 6; side++) {
			int col = FACE_POS[side][0];
			int row = FACE_POS[side][1];
			int fx = guiX + 28 + col * (FACE_SIZE + GAP);
			int fy = startY + row * (FACE_SIZE + GAP);

			for (int type = 0; type < 4; type++) {
				int cx = fx + 4 + (type % 2) * (CELL_SIZE + 2);
				int cy = fy + 14 + (type / 2) * (CELL_SIZE + 2);
				if (mouseX >= cx && mouseX < cx + CELL_SIZE && mouseY >= cy && mouseY < cy + CELL_SIZE) {
					int[] config = tile.getSideConfig();
					int oldMode = Aero_SideConfig.get(config, side, type);
					int[] allowed = tile.getAllowedModes(type);
					int newMode = cycleAllowed(oldMode, allowed);
					tile.setSideMode(side, type, newMode);
					tile.worldObj.markBlockNeedsUpdate(tile.xCoord, tile.yCoord, tile.zCoord);
					for (int[] d : new int[][]{{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}}) {
						tile.worldObj.markBlockNeedsUpdate(tile.xCoord+d[0], tile.yCoord+d[1], tile.zCoord+d[2]);
					}
					return;
				}
			}
		}

		super.mouseClicked(mouseX, mouseY, button);
	}

	public boolean doesGuiPauseGame() {
		return false;
	}
}
