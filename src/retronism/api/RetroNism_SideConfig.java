package retronism.api;

public class RetroNism_SideConfig {
	// IO Modes
	public static final int MODE_NONE = 0;
	public static final int MODE_INPUT = 1;
	public static final int MODE_OUTPUT = 2;
	public static final int MODE_INPUT_OUTPUT = 3;

	// Type indices (for sideConfig array: index = side*4 + type)
	public static final int TYPE_ENERGY = 0;
	public static final int TYPE_FLUID = 1;
	public static final int TYPE_GAS = 2;
	public static final int TYPE_ITEM = 3;
	public static final int TYPE_COUNT = 4;

	// Side indices (standard Minecraft)
	public static final int SIDE_BOTTOM = 0;
	public static final int SIDE_TOP = 1;
	public static final int SIDE_NORTH = 2;
	public static final int SIDE_SOUTH = 3;
	public static final int SIDE_WEST = 4;
	public static final int SIDE_EAST = 5;
	public static final int SIDE_COUNT = 6;

	// Colors for GUI rendering (ARGB)
	public static final int COLOR_NONE = 0xFF555555;
	public static final int COLOR_ENERGY_IN = 0xFFD4AA00;
	public static final int COLOR_ENERGY_OUT = 0xFFFFDD55;
	public static final int COLOR_ENERGY_IO = 0xFFEEC833;
	public static final int COLOR_FLUID_IN = 0xFF3366FF;
	public static final int COLOR_FLUID_OUT = 0xFF88BBFF;
	public static final int COLOR_FLUID_IO = 0xFF5590FF;
	public static final int COLOR_GAS_IN = 0xFF888888;
	public static final int COLOR_GAS_OUT = 0xFFCCCCCC;
	public static final int COLOR_GAS_IO = 0xFFAAAAAA;
	public static final int COLOR_ITEM_IN = 0xFFFF8800;
	public static final int COLOR_ITEM_OUT = 0xFFFFBB55;
	public static final int COLOR_ITEM_IO = 0xFFFF9F2A;

	public static int getColor(int type, int mode) {
		if (mode == MODE_NONE) return COLOR_NONE;
		switch (type) {
			case TYPE_ENERGY:
				return mode == MODE_INPUT ? COLOR_ENERGY_IN : mode == MODE_OUTPUT ? COLOR_ENERGY_OUT : COLOR_ENERGY_IO;
			case TYPE_FLUID:
				return mode == MODE_INPUT ? COLOR_FLUID_IN : mode == MODE_OUTPUT ? COLOR_FLUID_OUT : COLOR_FLUID_IO;
			case TYPE_GAS:
				return mode == MODE_INPUT ? COLOR_GAS_IN : mode == MODE_OUTPUT ? COLOR_GAS_OUT : COLOR_GAS_IO;
			case TYPE_ITEM:
				return mode == MODE_INPUT ? COLOR_ITEM_IN : mode == MODE_OUTPUT ? COLOR_ITEM_OUT : COLOR_ITEM_IO;
			default: return COLOR_NONE;
		}
	}

	public static String getTypeName(int type) {
		switch (type) {
			case TYPE_ENERGY: return "Energy";
			case TYPE_FLUID: return "Fluid";
			case TYPE_GAS: return "Gas";
			case TYPE_ITEM: return "Item";
			default: return "?";
		}
	}

	public static String getModeName(int mode) {
		switch (mode) {
			case MODE_INPUT: return "Input";
			case MODE_OUTPUT: return "Output";
			case MODE_INPUT_OUTPUT: return "I/O";
			default: return "Off";
		}
	}

	public static String getSideName(int side) {
		switch (side) {
			case SIDE_BOTTOM: return "Bottom";
			case SIDE_TOP: return "Top";
			case SIDE_NORTH: return "North";
			case SIDE_SOUTH: return "South";
			case SIDE_WEST: return "West";
			case SIDE_EAST: return "East";
			default: return "?";
		}
	}

	// Cycle mode: NONE -> INPUT -> OUTPUT -> INPUT_OUTPUT -> NONE
	public static int cycleMode(int current) {
		return (current + 1) % 4;
	}

	public static int oppositeSide(int side) {
		return side ^ 1; // 0<->1, 2<->3, 4<->5
	}

	public static boolean canOutput(int mode) {
		return mode == MODE_OUTPUT || mode == MODE_INPUT_OUTPUT;
	}

	public static boolean canInput(int mode) {
		return mode == MODE_INPUT || mode == MODE_INPUT_OUTPUT;
	}

	// Helper: get/set from flat array (int[24])
	public static int get(int[] config, int side, int type) {
		return config[side * TYPE_COUNT + type];
	}

	public static void set(int[] config, int side, int type, int mode) {
		config[side * TYPE_COUNT + type] = mode;
	}
}
