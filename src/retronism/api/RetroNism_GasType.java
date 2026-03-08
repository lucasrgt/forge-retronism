package retronism.api;

public class Retronism_GasType {
	public static final int NONE = 0;
	public static final int HYDROGEN = 1;
	public static final int OXYGEN = 2;
	public static final int OZONE = 3;

	public static String getName(int type) {
		switch (type) {
			case HYDROGEN: return "Hydrogen";
			case OXYGEN: return "Oxygen";
			case OZONE: return "Ozone";
			default: return "None";
		}
	}

	public static int getColor(int type) {
		switch (type) {
			case HYDROGEN: return 0xFF88BBFF;
			case OXYGEN: return 0xFFFF8888;
			case OZONE: return 0xFF99DDFF;
			default: return 0xFFFFFFFF;
		}
	}
}
