package retronism.tile;

import net.minecraft.src.*;
import aero.machineapi.*;

public class Retronism_TileCable extends TileEntity implements Aero_IEnergyReceiver, Aero_ISideConfigurable {
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
			Aero_SideConfig.set(sideConfig, s, Aero_SideConfig.TYPE_ENERGY, Aero_SideConfig.MODE_INPUT_OUTPUT);
		}
	}

	public int[] getSideConfig() { return sideConfig; }
	public void setSideMode(int side, int type, int mode) {
		if (!supportsType(type)) return;
		int[] allowed = getAllowedModes(type);
		for (int m : allowed) { if (m == mode) { Aero_SideConfig.set(sideConfig, side, type, mode); return; } }
	}
	public boolean supportsType(int type) {
		return type == Aero_SideConfig.TYPE_ENERGY;
	}
	public int[] getAllowedModes(int type) {
		if (type == Aero_SideConfig.TYPE_ENERGY) return new int[]{Aero_SideConfig.MODE_NONE, Aero_SideConfig.MODE_INPUT, Aero_SideConfig.MODE_OUTPUT, Aero_SideConfig.MODE_INPUT_OUTPUT};
		return new int[]{Aero_SideConfig.MODE_NONE};
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
		return Aero_SideConfig.get(sideConfig, side, Aero_SideConfig.TYPE_ENERGY);
	}

	private boolean canSendTo(int side, TileEntity te) {
		if (!Aero_SideConfig.canOutput(getSideMode(side))) return false;
		int oppSide = Aero_SideConfig.oppositeSide(side);
		if (te instanceof Aero_ISideConfigurable) {
			int neighborMode = Aero_SideConfig.get(((Aero_ISideConfigurable) te).getSideConfig(), oppSide, Aero_SideConfig.TYPE_ENERGY);
			if (!Aero_SideConfig.canInput(neighborMode)) return false;
		}
		return true;
	}

	public void updateEntity() {
		receivedThisTick = 0;
		if (this.worldObj.multiplayerWorld || storedEnergy <= 0) return;

		int receivers = 0;

		for (int side = 0; side < 6; side++) {
			int[] d = DIRS[side];
			int nx = xCoord + d[0], ny = yCoord + d[1], nz = zCoord + d[2];
			TileEntity te = Aero_PortRegistry.resolveHandler(worldObj, nx, ny, nz);
			if (te == null) continue;
			if (!canSendTo(side, te)) continue;
			if (te instanceof Aero_IEnergyReceiver && !(te instanceof Retronism_TileCable)) {
				Aero_IEnergyReceiver recv = (Aero_IEnergyReceiver) te;
				if (recv.getStoredEnergy() < recv.getMaxEnergy()) receivers++;
			} else if (te instanceof Retronism_TileCable) {
				if (((Retronism_TileCable) te).storedEnergy < this.storedEnergy) receivers++;
			}
		}

		if (receivers == 0) return;

		int perReceiver = Math.min(TRANSFER_RATE, storedEnergy) / receivers;
		if (perReceiver <= 0) perReceiver = 1;

		for (int side = 0; side < 6; side++) {
			if (storedEnergy <= 0) break;
			int[] d = DIRS[side];
			int nx = xCoord + d[0], ny = yCoord + d[1], nz = zCoord + d[2];
			TileEntity te = Aero_PortRegistry.resolveHandler(worldObj, nx, ny, nz);
			if (te == null) continue;
			if (!canSendTo(side, te)) continue;
			if (te instanceof Aero_IEnergyReceiver && !(te instanceof Retronism_TileCable)) {
				Aero_IEnergyReceiver recv = (Aero_IEnergyReceiver) te;
				if (recv.getStoredEnergy() < recv.getMaxEnergy()) {
					int toSend = Math.min(perReceiver, storedEnergy);
					storedEnergy -= recv.receiveEnergy(toSend);
				}
			} else if (te instanceof Retronism_TileCable) {
				Retronism_TileCable otherCable = (Retronism_TileCable) te;
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
