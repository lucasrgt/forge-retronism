package retronism.api;

public interface Retronism_ISideConfigurable {
	int[] getSideConfig();
	void setSideMode(int side, int type, int mode);
	boolean supportsType(int type);
	/** Returns allowed modes for a type. Default: all modes. */
	int[] getAllowedModes(int type);
}
