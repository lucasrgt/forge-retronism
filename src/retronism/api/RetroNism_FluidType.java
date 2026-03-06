package retronism.api;

public class RetroNism_FluidType {
	public static final int NONE = 0;
	public static final int WATER = 1;

	public static String getName(int type) {
		switch (type) {
			case WATER: return "Water";
			default: return "None";
		}
	}

	public static int getColor(int type) {
		switch (type) {
			case WATER: return 0xFF3344FF;
			default: return 0xFFFFFFFF;
		}
	}
}
