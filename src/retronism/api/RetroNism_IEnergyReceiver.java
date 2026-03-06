package retronism.api;

public interface RetroNism_IEnergyReceiver {
	int receiveEnergy(int amount);
	int getStoredEnergy();
	int getMaxEnergy();
}
