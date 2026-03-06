package retronism.tile;

import net.minecraft.src.*;
import retronism.api.*;

public class RetroNism_TileFluidPipe extends TileEntity implements RetroNism_IFluidHandler {
	private int fluidType = RetroNism_FluidType.NONE;
	private int fluidAmount = 0;
	private static final int MAX_FLUID = 500;
	private static final int TRANSFER_RATE = 200;
	private int receivedThisTick = 0;

	public int receiveFluid(int type, int amountMB) {
		if (type == RetroNism_FluidType.NONE) return 0;
		if (fluidType != RetroNism_FluidType.NONE && fluidType != type) return 0;
		int canReceive = TRANSFER_RATE - receivedThisTick;
		if (canReceive <= 0) return 0;
		int space = MAX_FLUID - fluidAmount;
		int accepted = Math.min(amountMB, Math.min(space, canReceive));
		if (accepted > 0) {
			fluidType = type;
			fluidAmount += accepted;
			receivedThisTick += accepted;
		}
		return accepted;
	}

	public int extractFluid(int type, int amountMB) {
		if (fluidType != type || fluidAmount <= 0) return 0;
		int extracted = Math.min(amountMB, fluidAmount);
		fluidAmount -= extracted;
		if (fluidAmount == 0) fluidType = RetroNism_FluidType.NONE;
		return extracted;
	}

	public int getFluidType() { return fluidType; }
	public int getFluidAmount() { return fluidAmount; }
	public int getFluidCapacity() { return MAX_FLUID; }

	public void updateEntity() {
		receivedThisTick = 0;
		if (this.worldObj.multiplayerWorld || fluidAmount <= 0) return;

		int[][] dirs = {{-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1}};
		int receivers = 0;

		for (int[] d : dirs) {
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te instanceof RetroNism_IFluidHandler && !(te instanceof RetroNism_TileFluidPipe)) {
				RetroNism_IFluidHandler handler = (RetroNism_IFluidHandler) te;
				if (handler.getFluidAmount() < handler.getFluidCapacity()) {
					receivers++;
				}
			} else if (te instanceof RetroNism_TileFluidPipe) {
				RetroNism_TileFluidPipe other = (RetroNism_TileFluidPipe) te;
				if (other.fluidAmount < this.fluidAmount) {
					receivers++;
				}
			}
		}

		if (receivers == 0) return;

		int perReceiver = Math.min(TRANSFER_RATE, fluidAmount) / receivers;
		if (perReceiver <= 0) perReceiver = 1;

		for (int[] d : dirs) {
			if (fluidAmount <= 0) break;
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te instanceof RetroNism_IFluidHandler && !(te instanceof RetroNism_TileFluidPipe)) {
				RetroNism_IFluidHandler handler = (RetroNism_IFluidHandler) te;
				if (handler.getFluidAmount() < handler.getFluidCapacity()) {
					int toSend = Math.min(perReceiver, fluidAmount);
					int accepted = handler.receiveFluid(fluidType, toSend);
					fluidAmount -= accepted;
				}
			} else if (te instanceof RetroNism_TileFluidPipe) {
				RetroNism_TileFluidPipe other = (RetroNism_TileFluidPipe) te;
				if (other.fluidAmount < this.fluidAmount) {
					int toSend = Math.min(perReceiver, fluidAmount);
					int accepted = other.receiveFluid(fluidType, toSend);
					fluidAmount -= accepted;
				}
			}
		}

		if (fluidAmount == 0) fluidType = RetroNism_FluidType.NONE;
	}

	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		fluidType = nbt.getInteger("FluidType");
		fluidAmount = nbt.getInteger("FluidAmount");
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("FluidType", fluidType);
		nbt.setInteger("FluidAmount", fluidAmount);
	}
}
