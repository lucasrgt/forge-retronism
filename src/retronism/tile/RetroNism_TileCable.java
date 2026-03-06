package retronism.tile;

import net.minecraft.src.*;
import retronism.api.*;

public class RetroNism_TileCable extends TileEntity implements RetroNism_IEnergyReceiver, RetroNism_ISideConfigurable {
	private int storedEnergy = 0;
	private static final int MAX_ENERGY = 800;
	private static final int TRANSFER_RATE = 200;
	private int receivedThisTick = 0;
	private int[] sideConfig = new int[24];

	// Ordered to match SideConfig: Bottom(0), Top(1), North(2), South(3), West(4), East(5)
	private static final int[][] DIRS = {
		{0,-1,0}, {0,1,0}, {0,0,-1}, {0,0,1}, {-1,0,0}, {1,0,0}
	};

	{
		for (int s = 0; s < 6; s++) {
			RetroNism_SideConfig.set(sideConfig, s, RetroNism_SideConfig.TYPE_ENERGY, RetroNism_SideConfig.MODE_INPUT_OUTPUT);
		}
	}

	public int[] getSideConfig() { return sideConfig; }
	public void setSideMode(int side, int type, int mode) {
		if (supportsType(type)) RetroNism_SideConfig.set(sideConfig, side, type, mode);
	}
	public boolean supportsType(int type) {
		return type == RetroNism_SideConfig.TYPE_ENERGY;
	}

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

	public int getSideMode(int side) {
		return RetroNism_SideConfig.get(sideConfig, side, RetroNism_SideConfig.TYPE_ENERGY);
	}

	private boolean canSendTo(int side, TileEntity te) {
		if (!RetroNism_SideConfig.canOutput(getSideMode(side))) return false;
		int oppSide = RetroNism_SideConfig.oppositeSide(side);
		if (te instanceof RetroNism_ISideConfigurable) {
			int neighborMode = RetroNism_SideConfig.get(((RetroNism_ISideConfigurable) te).getSideConfig(), oppSide, RetroNism_SideConfig.TYPE_ENERGY);
			if (!RetroNism_SideConfig.canInput(neighborMode)) return false;
		}
		return true;
	}

	public void updateEntity() {
		receivedThisTick = 0;
		if (this.worldObj.multiplayerWorld || storedEnergy <= 0) return;

		int receivers = 0;

		for (int side = 0; side < 6; side++) {
			int[] d = DIRS[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te == null) continue;
			if (!canSendTo(side, te)) continue;
			if (te instanceof RetroNism_IEnergyReceiver && !(te instanceof RetroNism_TileCable)) {
				RetroNism_IEnergyReceiver recv = (RetroNism_IEnergyReceiver) te;
				if (recv.getStoredEnergy() < recv.getMaxEnergy()) receivers++;
			} else if (te instanceof RetroNism_TileCable) {
				if (((RetroNism_TileCable) te).storedEnergy < this.storedEnergy) receivers++;
			}
		}

		if (receivers == 0) return;

		int perReceiver = Math.min(TRANSFER_RATE, storedEnergy) / receivers;
		if (perReceiver <= 0) perReceiver = 1;

		for (int side = 0; side < 6; side++) {
			if (storedEnergy <= 0) break;
			int[] d = DIRS[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te == null) continue;
			if (!canSendTo(side, te)) continue;
			if (te instanceof RetroNism_IEnergyReceiver && !(te instanceof RetroNism_TileCable)) {
				RetroNism_IEnergyReceiver recv = (RetroNism_IEnergyReceiver) te;
				if (recv.getStoredEnergy() < recv.getMaxEnergy()) {
					int toSend = Math.min(perReceiver, storedEnergy);
					storedEnergy -= recv.receiveEnergy(toSend);
				}
			} else if (te instanceof RetroNism_TileCable) {
				RetroNism_TileCable otherCable = (RetroNism_TileCable) te;
				if (otherCable.storedEnergy < this.storedEnergy) {
					int toSend = Math.min(perReceiver, storedEnergy);
					storedEnergy -= otherCable.receiveEnergy(toSend);
				}
			}
		}
	}

	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		storedEnergy = nbt.getShort("Energy");
		if (nbt.hasKey("SC0")) {
			for (int i = 0; i < 24; i++) this.sideConfig[i] = nbt.getInteger("SC" + i);
		}
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setShort("Energy", (short) storedEnergy);
		for (int i = 0; i < 24; i++) nbt.setInteger("SC" + i, this.sideConfig[i]);
	}
}
