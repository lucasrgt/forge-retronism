package retronism.api;

public interface RetroNism_IGasHandler {
	int receiveGas(int gasType, int amountMB);
	int extractGas(int gasType, int amountMB);
	int getGasType();
	int getGasAmount();
	int getGasCapacity();
}
