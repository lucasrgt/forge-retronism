package retronism.gui;

import net.minecraft.src.*;
import aero.machineapi.*;
import retronism.tile.*;
import org.lwjgl.opengl.GL11;

public class Retronism_GuiPipeConfig extends GuiScreen {
	private Aero_ISideConfigurable tile;
	private EntityPlayer player;
	private int selectedType = -1;
	private int selectedTab = 0; // 0 = Config, 1 = Filters
	private static RenderItem itemRenderer = new RenderItem();

	private static final int GUI_WIDTH = 176;
	private static final int GUI_HEIGHT = 166;
	private static final int FACE_SIZE = 36;
	private static final int FACE_GAP = 4;
	private static final int CROSS_START_Y = 34;

	private static final int TAB_WIDTH = 60;
	private static final int TAB_HEIGHT = 16;

	// Cube-net layout: {col, row}
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

	// Filter tab constants
	private static final int GHOST_SLOT_SIZE = 18;
	private static final int GHOST_GRID_X = 8;
	private static final int GHOST_GRID_Y = 24;
	private static final int FILTER_BTN_X = 66;
	private static final int FILTER_BTN_Y = 24;
	private static final int FILTER_BTN_W = 58;
	private static final int FILTER_BTN_H = 14;
	private static final int PRIO_LABEL_Y = 44;

	public Retronism_GuiPipeConfig(EntityPlayer player, Aero_ISideConfigurable tile) {
		this.player = player;
		this.tile = tile;
		this.selectedType = getFirstSupportedType();
	}

	private boolean hasFilterTab() {
		return tile instanceof Retronism_TileItemPipe;
	}

	private Retronism_TileItemPipe getItemPipe() {
		return (Retronism_TileItemPipe) tile;
	}

	private int getFirstSupportedType() {
		for (int t = 0; t < Aero_SideConfig.TYPE_COUNT; t++) {
			if (tile.supportsType(t)) return t;
		}
		return 0;
	}

	private int getNextSupportedType(int current) {
		for (int i = 1; i <= Aero_SideConfig.TYPE_COUNT; i++) {
			int next = (current + i) % Aero_SideConfig.TYPE_COUNT;
			if (tile.supportsType(next)) return next;
		}
		return current;
	}

	private int countSupportedTypes() {
		int count = 0;
		for (int t = 0; t < Aero_SideConfig.TYPE_COUNT; t++) {
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

		// Panel background
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		int textureID = this.mc.renderEngine.getTexture("/gui/retronism_side_config.png");
		this.mc.renderEngine.bindTexture(textureID);
		this.drawTexturedModalRect(guiX, guiY, 0, 0, GUI_WIDTH, GUI_HEIGHT);

		// Draw tabs
		if (hasFilterTab()) {
			drawTabs(guiX, guiY, mouseX, mouseY);
		}

		if (selectedTab == 0) {
			drawConfigTab(guiX, guiY, mouseX, mouseY);
		} else {
			drawFilterTab(guiX, guiY, mouseX, mouseY);
		}

		super.drawScreen(mouseX, mouseY, partialTick);
	}

	private void drawTabs(int guiX, int guiY, int mouseX, int mouseY) {
		int tabY = guiY - TAB_HEIGHT + 2;
		String[] tabNames = {"Config", "Filters"};
		for (int t = 0; t < 2; t++) {
			int tabX = guiX + 4 + t * (TAB_WIDTH + 2);
			boolean selected = (t == selectedTab);
			int bgColor = selected ? 0xFFC6C6C6 : 0xFF8B8B8B;
			int borderColor = 0xFF000000;

			// Tab background
			drawRect(tabX, tabY, tabX + TAB_WIDTH, tabY + TAB_HEIGHT, borderColor);
			drawRect(tabX + 1, tabY + 1, tabX + TAB_WIDTH - 1, tabY + TAB_HEIGHT + (selected ? 1 : 0), bgColor);

			// Tab label
			int labelW = this.fontRenderer.getStringWidth(tabNames[t]);
			int labelColor = selected ? 0x404040 : 0xFFFFFF;
			this.fontRenderer.drawString(tabNames[t], tabX + (TAB_WIDTH - labelW) / 2, tabY + 4, labelColor);
		}
	}

	private void drawConfigTab(int guiX, int guiY, int mouseX, int mouseY) {
		// Title
		this.fontRenderer.drawString("Pipe Config", guiX + 8, guiY + 6, 4210752);

		// Type selector button
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

			int mode = Aero_SideConfig.get(config, side, selectedType);
			int s = FACE_SIZE;

			drawRect(fx, fy, fx + s, fy + s, 0xFF000000);
			drawRect(fx + 1, fy + 1, fx + s - 1, fy + s - 1, MODE_SHADOW[mode]);
			drawRect(fx + 1, fy + 1, fx + s - 2, fy + 2, MODE_HILITE[mode]);
			drawRect(fx + 1, fy + 1, fx + 2, fy + s - 2, MODE_HILITE[mode]);
			drawRect(fx + 2, fy + 2, fx + s - 2, fy + s - 2, MODE_FILL[mode]);

			String sideLabel = SIDE_LABELS[side];
			int labelW = this.fontRenderer.getStringWidth(sideLabel);
			this.fontRenderer.drawStringWithShadow(sideLabel, fx + (FACE_SIZE - labelW) / 2, fy + 6, 0xFFFFFF);

			String modeLabel = MODE_LABELS[mode];
			int modeW = this.fontRenderer.getStringWidth(modeLabel);
			int modeColor = (mode == Aero_SideConfig.MODE_NONE) ? 0x999999 : 0xFFFFFF;
			this.fontRenderer.drawStringWithShadow(modeLabel, fx + (FACE_SIZE - modeW) / 2, fy + 20, modeColor);
		}

		// Tooltips
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
				int tw = this.fontRenderer.getStringWidth(tip);
				int tx = mouseX + 8;
				int ty = mouseY - 12;
				drawRect(tx - 2, ty - 2, tx + tw + 2, ty + 10, 0xCC000000);
				this.fontRenderer.drawStringWithShadow(tip, tx, ty, 0xFFFFFF);
			}
		}

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
	}

	private void drawFilterTab(int guiX, int guiY, int mouseX, int mouseY) {
		Retronism_TileItemPipe pipe = getItemPipe();

		// Title
		this.fontRenderer.drawString("Filters & Priority", guiX + 8, guiY + 6, 4210752);

		// 3x3 ghost slot grid
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int i = row * 3 + col;
				int sx = guiX + GHOST_GRID_X + col * GHOST_SLOT_SIZE;
				int sy = guiY + GHOST_GRID_Y + row * GHOST_SLOT_SIZE;

				// Slot border (MC style)
				drawRect(sx, sy, sx + GHOST_SLOT_SIZE, sy + GHOST_SLOT_SIZE, 0xFF000000);
				drawRect(sx + 1, sy + 1, sx + GHOST_SLOT_SIZE - 1, sy + 1 + 1, 0xFF555555);
				drawRect(sx + 1, sy + 1, sx + 1 + 1, sy + GHOST_SLOT_SIZE - 1, 0xFF555555);
				drawRect(sx + 1, sy + GHOST_SLOT_SIZE - 2, sx + GHOST_SLOT_SIZE - 1, sy + GHOST_SLOT_SIZE - 1, 0xFFFFFFFF);
				drawRect(sx + GHOST_SLOT_SIZE - 2, sy + 1, sx + GHOST_SLOT_SIZE - 1, sy + GHOST_SLOT_SIZE - 1, 0xFFFFFFFF);
				drawRect(sx + 2, sy + 2, sx + GHOST_SLOT_SIZE - 2, sy + GHOST_SLOT_SIZE - 2, 0xFF8B8B8B);

				// Draw ghost item
				ItemStack filterItem = pipe.getFilterSlot(i);
				if (filterItem != null) {
					drawItemStack(filterItem, sx + 1, sy + 1);
				}
			}
		}

		// Whitelist/Blacklist button
		{
			int bx = guiX + FILTER_BTN_X;
			int by = guiY + FILTER_BTN_Y;
			boolean wl = pipe.isWhitelist();
			int btnColor = wl ? 0xFF33AA33 : 0xFFCC3333;
			String label = wl ? "Whitelist" : "Blacklist";
			drawRect(bx, by, bx + FILTER_BTN_W, by + FILTER_BTN_H, 0xFF000000);
			drawRect(bx + 1, by + 1, bx + FILTER_BTN_W - 1, by + FILTER_BTN_H - 1, 0xFF555555);
			drawRect(bx + 1, by + 1, bx + FILTER_BTN_W - 2, by + 2, 0xFFFFFFFF);
			drawRect(bx + 1, by + 1, bx + 2, by + FILTER_BTN_H - 2, 0xFFFFFFFF);
			drawRect(bx + 2, by + 2, bx + FILTER_BTN_W - 2, by + FILTER_BTN_H - 2, btnColor);
			int lw = this.fontRenderer.getStringWidth(label);
			this.fontRenderer.drawStringWithShadow(label, bx + (FILTER_BTN_W - lw) / 2, by + 3, 0xFFFFFF);
		}

		// Global priority
		{
			int px = guiX + FILTER_BTN_X;
			int py = guiY + PRIO_LABEL_Y;
			this.fontRenderer.drawString("Priority:", px, py, 4210752);
			int numX = px + 52;
			int numY = py;
			drawPriorityBox(numX, numY, pipe.getPriority());
		}

		// Per-side priorities
		{
			int baseY = guiY + 68;
			this.fontRenderer.drawString("Side Priority", guiX + 8, baseY - 12, 4210752);
			for (int side = 0; side < 6; side++) {
				int col = side % 3;
				int row = side / 3;
				int lx = guiX + 8 + col * 56;
				int ly = baseY + row * 16;
				this.fontRenderer.drawString(SIDE_LABELS[side] + ":", lx, ly + 2, 4210752);
				int numX = lx + 22;
				drawPriorityBox(numX, ly, pipe.getSidePriority(side));
			}
		}

		// Tooltips for ghost slots
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int i = row * 3 + col;
				int sx = guiX + GHOST_GRID_X + col * GHOST_SLOT_SIZE;
				int sy = guiY + GHOST_GRID_Y + row * GHOST_SLOT_SIZE;
				if (mouseX >= sx && mouseX < sx + GHOST_SLOT_SIZE && mouseY >= sy && mouseY < sy + GHOST_SLOT_SIZE) {
					ItemStack filterItem = pipe.getFilterSlot(i);
					if (filterItem != null) {
						String name = filterItem.getItemName();
						int tw = this.fontRenderer.getStringWidth(name);
						int tx = mouseX + 8;
						int ty = mouseY - 12;
						drawRect(tx - 2, ty - 2, tx + tw + 2, ty + 10, 0xCC000000);
						this.fontRenderer.drawStringWithShadow(name, tx, ty, 0xFFFFFF);
					} else {
						String tip = "Click with item to set filter";
						int tw = this.fontRenderer.getStringWidth(tip);
						int tx = mouseX + 8;
						int ty = mouseY - 12;
						drawRect(tx - 2, ty - 2, tx + tw + 2, ty + 10, 0xCC000000);
						this.fontRenderer.drawStringWithShadow(tip, tx, ty, 0xAAAAAA);
					}
				}
			}
		}

		// Tooltip for whitelist/blacklist
		{
			int bx = guiX + FILTER_BTN_X;
			int by = guiY + FILTER_BTN_Y;
			if (mouseX >= bx && mouseX < bx + FILTER_BTN_W && mouseY >= by && mouseY < by + FILTER_BTN_H) {
				String tip = pipe.isWhitelist() ? "Only filtered items pass" : "Filtered items are blocked";
				int tw = this.fontRenderer.getStringWidth(tip);
				int tx = mouseX + 8;
				int ty = mouseY - 12;
				drawRect(tx - 2, ty - 2, tx + tw + 2, ty + 10, 0xCC000000);
				this.fontRenderer.drawStringWithShadow(tip, tx, ty, 0xFFFFFF);
			}
		}
	}

	private void drawPriorityBox(int x, int y, int value) {
		int boxW = 16;
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

	private void drawItemStack(ItemStack stack, int x, int y) {
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		itemRenderer.renderItemIntoGUI(this.fontRenderer, this.mc.renderEngine, stack, x, y);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
	}

	protected void mouseClicked(int mouseX, int mouseY, int button) {
		int guiX = (this.width - GUI_WIDTH) / 2;
		int guiY = (this.height - GUI_HEIGHT) / 2;

		// Tab clicks
		if (hasFilterTab()) {
			int tabY = guiY - TAB_HEIGHT + 2;
			for (int t = 0; t < 2; t++) {
				int tabX = guiX + 4 + t * (TAB_WIDTH + 2);
				if (mouseX >= tabX && mouseX < tabX + TAB_WIDTH && mouseY >= tabY && mouseY < tabY + TAB_HEIGHT) {
					selectedTab = t;
					return;
				}
			}
		}

		if (selectedTab == 0) {
			handleConfigClick(guiX, guiY, mouseX, mouseY, button);
		} else {
			handleFilterClick(guiX, guiY, mouseX, mouseY, button);
		}
	}

	private void handleConfigClick(int guiX, int guiY, int mouseX, int mouseY, int button) {
		// Type button
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
				int oldMode = Aero_SideConfig.get(config, side, selectedType);
				int[] allowed = tile.getAllowedModes(selectedType);
				int newMode = cycleAllowed(oldMode, allowed);
				tile.setSideMode(side, selectedType, newMode);
				markDirty();
				return;
			}
		}

		super.mouseClicked(mouseX, mouseY, button);
	}

	private void handleFilterClick(int guiX, int guiY, int mouseX, int mouseY, int button) {
		Retronism_TileItemPipe pipe = getItemPipe();

		// Ghost slot clicks
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int i = row * 3 + col;
				int sx = guiX + GHOST_GRID_X + col * GHOST_SLOT_SIZE;
				int sy = guiY + GHOST_GRID_Y + row * GHOST_SLOT_SIZE;
				if (mouseX >= sx && mouseX < sx + GHOST_SLOT_SIZE && mouseY >= sy && mouseY < sy + GHOST_SLOT_SIZE) {
					ItemStack held = player.inventory.getItemStack();
					if (button == 1 || held == null) {
						// Right click or empty hand: clear slot
						pipe.setFilterSlot(i, null);
					} else {
						// Left click with item: set ghost filter
						pipe.setFilterSlot(i, new ItemStack(held.itemID, 1, held.getItemDamage()));
					}
					return;
				}
			}
		}

		// Whitelist/Blacklist button
		{
			int bx = guiX + FILTER_BTN_X;
			int by = guiY + FILTER_BTN_Y;
			if (mouseX >= bx && mouseX < bx + FILTER_BTN_W && mouseY >= by && mouseY < by + FILTER_BTN_H) {
				pipe.setWhitelist(!pipe.isWhitelist());
				return;
			}
		}

		// Global priority click
		{
			int px = guiX + FILTER_BTN_X + 52;
			int py = guiY + PRIO_LABEL_Y;
			if (mouseX >= px && mouseX < px + 16 && mouseY >= py && mouseY < py + 12) {
				if (button == 0) {
					pipe.setPriority((pipe.getPriority() + 1) % 10);
				} else {
					pipe.setPriority((pipe.getPriority() + 9) % 10);
				}
				return;
			}
		}

		// Per-side priority clicks
		{
			int baseY = guiY + 68;
			for (int side = 0; side < 6; side++) {
				int col = side % 3;
				int row = side / 3;
				int numX = guiX + 8 + col * 56 + 22;
				int numY = baseY + row * 16;
				if (mouseX >= numX && mouseX < numX + 16 && mouseY >= numY && mouseY < numY + 12) {
					if (button == 0) {
						pipe.setSidePriority(side, (pipe.getSidePriority(side) + 1) % 10);
					} else {
						pipe.setSidePriority(side, (pipe.getSidePriority(side) + 9) % 10);
					}
					return;
				}
			}
		}

		super.mouseClicked(mouseX, mouseY, button);
	}

	private void markDirty() {
		if (tile instanceof TileEntity) {
			TileEntity te = (TileEntity) tile;
			te.worldObj.markBlockNeedsUpdate(te.xCoord, te.yCoord, te.zCoord);
			for (int[] d : new int[][]{{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}}) {
				te.worldObj.markBlockNeedsUpdate(te.xCoord+d[0], te.yCoord+d[1], te.zCoord+d[2]);
			}
		}
	}

	public boolean doesGuiPauseGame() {
		return false;
	}
}
