package retronism.api;

public interface Retronism_IEnergyReceiver {
	int receiveEnergy(int amount);
	int getStoredEnergy();
	int getMaxEnergy();
}
