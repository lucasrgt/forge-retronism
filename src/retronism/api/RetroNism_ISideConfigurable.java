package retronism.api;

public interface Retronism_ISideConfigurable {
	int[] getSideConfig();
	void setSideMode(int side, int type, int mode);
	boolean supportsType(int type);
}
