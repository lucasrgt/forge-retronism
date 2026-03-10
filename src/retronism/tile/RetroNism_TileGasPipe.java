package retronism.tile;

import net.minecraft.src.*;
import aero.machineapi.*;

public class Retronism_TileGasPipe extends TileEntity implements Aero_IGasHandler, Aero_ISideConfigurable {
	private int gasType = Aero_GasType.NONE;
	private int gasAmount = 0;
	private static final int MAX_GAS = 500;
	private static final int TRANSFER_RATE = 200;
	private int receivedThisTick = 0;
	private int[] sideConfig = new int[24];

	private static final int[][] DIRS = {
		{0,-1,0}, {0,1,0}, {0,0,-1}, {0,0,1}, {-1,0,0}, {1,0,0}
	};

	{
		for (int s = 0; s < 6; s++) {
			Aero_SideConfig.set(sideConfig, s, Aero_SideConfig.TYPE_GAS, Aero_SideConfig.MODE_INPUT_OUTPUT);
		}
	}

	public int[] getSideConfig() { return sideConfig; }
	public void setSideMode(int side, int type, int mode) {
		if (!supportsType(type)) return;
		int[] allowed = getAllowedModes(type);
		for (int m : allowed) { if (m == mode) { Aero_SideConfig.set(sideConfig, side, type, mode); return; } }
	}
	public boolean supportsType(int type) {
		return type == Aero_SideConfig.TYPE_GAS;
	}
	public int[] getAllowedModes(int type) {
		if (type == Aero_SideConfig.TYPE_GAS) return new int[]{Aero_SideConfig.MODE_NONE, Aero_SideConfig.MODE_INPUT, Aero_SideConfig.MODE_OUTPUT, Aero_SideConfig.MODE_INPUT_OUTPUT};
		return new int[]{Aero_SideConfig.MODE_NONE};
	}

	public int receiveGas(int type, int amountMB) {
		if (type == Aero_GasType.NONE) return 0;
		if (gasType != Aero_GasType.NONE && gasType != type) return 0;
		int canReceive = TRANSFER_RATE - receivedThisTick;
		if (canReceive <= 0) return 0;
		int space = MAX_GAS - gasAmount;
		int accepted = Math.min(amountMB, Math.min(space, canReceive));
		if (accepted > 0) {
			gasType = type;
			gasAmount += accepted;
			receivedThisTick += accepted;
		}
		return accepted;
	}

	public int extractGas(int type, int amountMB) {
		if (gasType != type || gasAmount <= 0) return 0;
		int extracted = Math.min(amountMB, gasAmount);
		gasAmount -= extracted;
		if (gasAmount == 0) gasType = Aero_GasType.NONE;
		return extracted;
	}

	public int getGasType() { return gasType; }
	public int getGasAmount() { return gasAmount; }
	public int getGasCapacity() { return MAX_GAS; }

	public int getSideMode(int side) {
		return Aero_SideConfig.get(sideConfig, side, Aero_SideConfig.TYPE_GAS);
	}

	private boolean canSendTo(int side, TileEntity te) {
		if (!Aero_SideConfig.canOutput(getSideMode(side))) return false;
		int oppSide = Aero_SideConfig.oppositeSide(side);
		if (te instanceof Aero_ISideConfigurable) {
			int neighborMode = Aero_SideConfig.get(((Aero_ISideConfigurable) te).getSideConfig(), oppSide, Aero_SideConfig.TYPE_GAS);
			if (!Aero_SideConfig.canInput(neighborMode)) return false;
		}
		return true;
	}

	public void updateEntity() {
		receivedThisTick = 0;
		if (this.worldObj.multiplayerWorld || gasAmount <= 0) return;

		int receivers = 0;

		for (int side = 0; side < 6; side++) {
			int[] d = DIRS[side];
			int nx = xCoord + d[0], ny = yCoord + d[1], nz = zCoord + d[2];
			TileEntity te = Aero_PortRegistry.resolveHandler(worldObj, nx, ny, nz);
			if (te == null) continue;
			if (!canSendTo(side, te)) continue;
			if (te instanceof Aero_IGasHandler && !(te instanceof Retronism_TileGasPipe)) {
				if (((Aero_IGasHandler) te).getGasAmount() < ((Aero_IGasHandler) te).getGasCapacity()) receivers++;
			} else if (te instanceof Retronism_TileGasPipe) {
				if (((Retronism_TileGasPipe) te).gasAmount < this.gasAmount) receivers++;
			}
		}

		if (receivers == 0) return;

		int perReceiver = Math.min(TRANSFER_RATE, gasAmount) / receivers;
		if (perReceiver <= 0) perReceiver = 1;

		for (int side = 0; side < 6; side++) {
			if (gasAmount <= 0) break;
			int[] d = DIRS[side];
			int nx = xCoord + d[0], ny = yCoord + d[1], nz = zCoord + d[2];
			TileEntity te = Aero_PortRegistry.resolveHandler(worldObj, nx, ny, nz);
			if (te == null) continue;
			if (!canSendTo(side, te)) continue;
			if (te instanceof Aero_IGasHandler && !(te instanceof Retronism_TileGasPipe)) {
				Aero_IGasHandler handler = (Aero_IGasHandler) te;
				if (handler.getGasAmount() < handler.getGasCapacity()) {
					int toSend = Math.min(perReceiver, gasAmount);
					gasAmount -= handler.receiveGas(gasType, toSend);
				}
			} else if (te instanceof Retronism_TileGasPipe) {
				Retronism_TileGasPipe other = (Retronism_TileGasPipe) te;
				if (other.gasAmount < this.gasAmount) {
					int toSend = Math.min(perReceiver, gasAmount);
					gasAmount -= other.receiveGas(gasType, toSend);
				}
			}
		}

		if (gasAmount == 0) gasType = Aero_GasType.NONE;
	}

	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		gasType = nbt.getInteger("GasType");
		gasAmount = nbt.getInteger("GasAmount");
		if (nbt.hasKey("SC0")) {
			for (int i = 0; i < 24; i++) this.sideConfig[i] = nbt.getInteger("SC" + i);
		}
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("GasType", gasType);
		nbt.setInteger("GasAmount", gasAmount);
		for (int i = 0; i < 24; i++) nbt.setInteger("SC" + i, this.sideConfig[i]);
	}
}
