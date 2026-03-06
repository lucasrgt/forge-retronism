package retronism.tile;

import net.minecraft.src.*;
import retronism.api.*;

public class RetroNism_TileGasPipe extends TileEntity implements RetroNism_IGasHandler {
	private int gasType = RetroNism_GasType.NONE;
	private int gasAmount = 0;
	private static final int MAX_GAS = 500;
	private static final int TRANSFER_RATE = 200;
	private int receivedThisTick = 0;

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

	public void updateEntity() {
		receivedThisTick = 0;
		if (this.worldObj.multiplayerWorld || gasAmount <= 0) return;

		int[][] dirs = {{-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1}};
		int receivers = 0;

		for (int[] d : dirs) {
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te instanceof RetroNism_IGasHandler && !(te instanceof RetroNism_TileGasPipe)) {
				RetroNism_IGasHandler handler = (RetroNism_IGasHandler) te;
				if (handler.getGasAmount() < handler.getGasCapacity()) {
					receivers++;
				}
			} else if (te instanceof RetroNism_TileGasPipe) {
				RetroNism_TileGasPipe other = (RetroNism_TileGasPipe) te;
				if (other.gasAmount < this.gasAmount) {
					receivers++;
				}
			}
		}

		if (receivers == 0) return;

		int perReceiver = Math.min(TRANSFER_RATE, gasAmount) / receivers;
		if (perReceiver <= 0) perReceiver = 1;

		for (int[] d : dirs) {
			if (gasAmount <= 0) break;
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te instanceof RetroNism_IGasHandler && !(te instanceof RetroNism_TileGasPipe)) {
				RetroNism_IGasHandler handler = (RetroNism_IGasHandler) te;
				if (handler.getGasAmount() < handler.getGasCapacity()) {
					int toSend = Math.min(perReceiver, gasAmount);
					int accepted = handler.receiveGas(gasType, toSend);
					gasAmount -= accepted;
				}
			} else if (te instanceof RetroNism_TileGasPipe) {
				RetroNism_TileGasPipe other = (RetroNism_TileGasPipe) te;
				if (other.gasAmount < this.gasAmount) {
					int toSend = Math.min(perReceiver, gasAmount);
					int accepted = other.receiveGas(gasType, toSend);
					gasAmount -= accepted;
				}
			}
		}

		if (gasAmount == 0) gasType = RetroNism_GasType.NONE;
	}

	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		gasType = nbt.getInteger("GasType");
		gasAmount = nbt.getInteger("GasAmount");
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("GasType", gasType);
		nbt.setInteger("GasAmount", gasAmount);
	}
}
