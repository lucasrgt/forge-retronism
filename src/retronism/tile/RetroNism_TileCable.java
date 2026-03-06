package retronism.tile;

import net.minecraft.src.*;
import retronism.api.*;

public class RetroNism_TileCable extends TileEntity implements RetroNism_IEnergyReceiver {
	private int storedEnergy = 0;
	private static final int MAX_ENERGY = 800;
	private static final int TRANSFER_RATE = 200;
	private int receivedThisTick = 0;

	public int receiveEnergy(int amount) {
		int canReceive = TRANSFER_RATE - receivedThisTick;
		if (canReceive <= 0) return 0;
		int space = MAX_ENERGY - storedEnergy;
		int accepted = Math.min(amount, Math.min(space, canReceive));
		storedEnergy += accepted;
		receivedThisTick += accepted;
		return accepted;
	}

	public int getStoredEnergy() {
		return storedEnergy;
	}

	public int getMaxEnergy() {
		return MAX_ENERGY;
	}

	public void updateEntity() {
		receivedThisTick = 0;
		if (this.worldObj.multiplayerWorld || storedEnergy <= 0) return;

		int[][] dirs = {{-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1}};
		int receivers = 0;

		// Count receivers that can accept energy (excluding cables with more energy)
		for (int[] d : dirs) {
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te instanceof RetroNism_IEnergyReceiver && !(te instanceof RetroNism_TileCable)) {
				RetroNism_IEnergyReceiver recv = (RetroNism_IEnergyReceiver) te;
				if (recv.getStoredEnergy() < recv.getMaxEnergy()) {
					receivers++;
				}
			} else if (te instanceof RetroNism_TileCable) {
				RetroNism_TileCable otherCable = (RetroNism_TileCable) te;
				if (otherCable.storedEnergy < this.storedEnergy) {
					receivers++;
				}
			}
		}

		if (receivers == 0) return;

		int perReceiver = Math.min(TRANSFER_RATE, storedEnergy) / receivers;
		if (perReceiver <= 0) perReceiver = 1;

		for (int[] d : dirs) {
			if (storedEnergy <= 0) break;
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te instanceof RetroNism_IEnergyReceiver && !(te instanceof RetroNism_TileCable)) {
				RetroNism_IEnergyReceiver recv = (RetroNism_IEnergyReceiver) te;
				if (recv.getStoredEnergy() < recv.getMaxEnergy()) {
					int toSend = Math.min(perReceiver, storedEnergy);
					int accepted = recv.receiveEnergy(toSend);
					storedEnergy -= accepted;
				}
			} else if (te instanceof RetroNism_TileCable) {
				RetroNism_TileCable otherCable = (RetroNism_TileCable) te;
				if (otherCable.storedEnergy < this.storedEnergy) {
					int toSend = Math.min(perReceiver, storedEnergy);
					int accepted = otherCable.receiveEnergy(toSend);
					storedEnergy -= accepted;
				}
			}
		}
	}

	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		storedEnergy = nbt.getShort("Energy");
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setShort("Energy", (short) storedEnergy);
	}
}
