package retronism.api;

public class Retronism_FluidType {
	public static final int NONE = 0;
	public static final int WATER = 1;
	public static final int HEAVY_WATER = 2;

	public static String getName(int type) {
		switch (type) {
			case WATER: return "Water";
			case HEAVY_WATER: return "Heavy Water";
			default: return "None";
		}
	}

	public static int getColor(int type) {
		switch (type) {
			case WATER: return 0xFF3344FF;
			case HEAVY_WATER: return 0xFF1A237E;
			default: return 0xFFFFFFFF;
		}
	}
}
