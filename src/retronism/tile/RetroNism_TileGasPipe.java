package retronism.tile;

import net.minecraft.src.*;
import retronism.api.*;

public class RetroNism_TileGasPipe extends TileEntity implements RetroNism_IGasHandler, RetroNism_ISideConfigurable {
	private int gasType = RetroNism_GasType.NONE;
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
			RetroNism_SideConfig.set(sideConfig, s, RetroNism_SideConfig.TYPE_GAS, RetroNism_SideConfig.MODE_INPUT_OUTPUT);
		}
	}

	public int[] getSideConfig() { return sideConfig; }
	public void setSideMode(int side, int type, int mode) {
		if (supportsType(type)) RetroNism_SideConfig.set(sideConfig, side, type, mode);
	}
	public boolean supportsType(int type) {
		return type == RetroNism_SideConfig.TYPE_GAS;
	}

	public int receiveGas(int type, int amountMB) {
		if (type == RetroNism_GasType.NONE) return 0;
		if (gasType != RetroNism_GasType.NONE && gasType != type) return 0;
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
		if (gasAmount == 0) gasType = RetroNism_GasType.NONE;
		return extracted;
	}

	public int getGasType() { return gasType; }
	public int getGasAmount() { return gasAmount; }
	public int getGasCapacity() { return MAX_GAS; }

	public int getSideMode(int side) {
		return RetroNism_SideConfig.get(sideConfig, side, RetroNism_SideConfig.TYPE_GAS);
	}

	private boolean canSendTo(int side, TileEntity te) {
		if (!RetroNism_SideConfig.canOutput(getSideMode(side))) return false;
		int oppSide = RetroNism_SideConfig.oppositeSide(side);
		if (te instanceof RetroNism_ISideConfigurable) {
			int neighborMode = RetroNism_SideConfig.get(((RetroNism_ISideConfigurable) te).getSideConfig(), oppSide, RetroNism_SideConfig.TYPE_GAS);
			if (!RetroNism_SideConfig.canInput(neighborMode)) return false;
		}
		return true;
	}

	public void updateEntity() {
		receivedThisTick = 0;
		if (this.worldObj.multiplayerWorld || gasAmount <= 0) return;

		int receivers = 0;

		for (int side = 0; side < 6; side++) {
			int[] d = DIRS[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te == null) continue;
			if (!canSendTo(side, te)) continue;
			if (te instanceof RetroNism_IGasHandler && !(te instanceof RetroNism_TileGasPipe)) {
				if (((RetroNism_IGasHandler) te).getGasAmount() < ((RetroNism_IGasHandler) te).getGasCapacity()) receivers++;
			} else if (te instanceof RetroNism_TileGasPipe) {
				if (((RetroNism_TileGasPipe) te).gasAmount < this.gasAmount) receivers++;
			}
		}

		if (receivers == 0) return;

		int perReceiver = Math.min(TRANSFER_RATE, gasAmount) / receivers;
		if (perReceiver <= 0) perReceiver = 1;

		for (int side = 0; side < 6; side++) {
			if (gasAmount <= 0) break;
			int[] d = DIRS[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te == null) continue;
			if (!canSendTo(side, te)) continue;
			if (te instanceof RetroNism_IGasHandler && !(te instanceof RetroNism_TileGasPipe)) {
				RetroNism_IGasHandler handler = (RetroNism_IGasHandler) te;
				if (handler.getGasAmount() < handler.getGasCapacity()) {
					int toSend = Math.min(perReceiver, gasAmount);
					gasAmount -= handler.receiveGas(gasType, toSend);
				}
			} else if (te instanceof RetroNism_TileGasPipe) {
				RetroNism_TileGasPipe other = (RetroNism_TileGasPipe) te;
				if (other.gasAmount < this.gasAmount) {
					int toSend = Math.min(perReceiver, gasAmount);
					gasAmount -= other.receiveGas(gasType, toSend);
				}
			}
		}

		if (gasAmount == 0) gasType = RetroNism_GasType.NONE;
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
