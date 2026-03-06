package retronism.tile;

import net.minecraft.src.*;
import retronism.api.*;

public class RetroNism_TileFluidPipe extends TileEntity implements RetroNism_IFluidHandler, RetroNism_ISideConfigurable {
	private int fluidType = RetroNism_FluidType.NONE;
	private int fluidAmount = 0;
	private static final int MAX_FLUID = 500;
	private static final int TRANSFER_RATE = 200;
	private int receivedThisTick = 0;
	private int[] sideConfig = new int[24];

	private static final int[][] DIRS = {
		{0,-1,0}, {0,1,0}, {0,0,-1}, {0,0,1}, {-1,0,0}, {1,0,0}
	};

	{
		for (int s = 0; s < 6; s++) {
			RetroNism_SideConfig.set(sideConfig, s, RetroNism_SideConfig.TYPE_FLUID, RetroNism_SideConfig.MODE_INPUT_OUTPUT);
		}
	}

	public int[] getSideConfig() { return sideConfig; }
	public void setSideMode(int side, int type, int mode) {
		if (supportsType(type)) RetroNism_SideConfig.set(sideConfig, side, type, mode);
	}
	public boolean supportsType(int type) {
		return type == RetroNism_SideConfig.TYPE_FLUID;
	}

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

	public int getSideMode(int side) {
		return RetroNism_SideConfig.get(sideConfig, side, RetroNism_SideConfig.TYPE_FLUID);
	}

	private boolean canSendTo(int side, TileEntity te) {
		if (!RetroNism_SideConfig.canOutput(getSideMode(side))) return false;
		int oppSide = RetroNism_SideConfig.oppositeSide(side);
		if (te instanceof RetroNism_ISideConfigurable) {
			int neighborMode = RetroNism_SideConfig.get(((RetroNism_ISideConfigurable) te).getSideConfig(), oppSide, RetroNism_SideConfig.TYPE_FLUID);
			if (!RetroNism_SideConfig.canInput(neighborMode)) return false;
		}
		return true;
	}

	public void updateEntity() {
		receivedThisTick = 0;
		if (this.worldObj.multiplayerWorld || fluidAmount <= 0) return;

		int receivers = 0;

		for (int side = 0; side < 6; side++) {
			int[] d = DIRS[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te == null) continue;
			if (!canSendTo(side, te)) continue;
			if (te instanceof RetroNism_IFluidHandler && !(te instanceof RetroNism_TileFluidPipe)) {
				if (((RetroNism_IFluidHandler) te).getFluidAmount() < ((RetroNism_IFluidHandler) te).getFluidCapacity()) receivers++;
			} else if (te instanceof RetroNism_TileFluidPipe) {
				if (((RetroNism_TileFluidPipe) te).fluidAmount < this.fluidAmount) receivers++;
			}
		}

		if (receivers == 0) return;

		int perReceiver = Math.min(TRANSFER_RATE, fluidAmount) / receivers;
		if (perReceiver <= 0) perReceiver = 1;

		for (int side = 0; side < 6; side++) {
			if (fluidAmount <= 0) break;
			int[] d = DIRS[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te == null) continue;
			if (!canSendTo(side, te)) continue;
			if (te instanceof RetroNism_IFluidHandler && !(te instanceof RetroNism_TileFluidPipe)) {
				RetroNism_IFluidHandler handler = (RetroNism_IFluidHandler) te;
				if (handler.getFluidAmount() < handler.getFluidCapacity()) {
					int toSend = Math.min(perReceiver, fluidAmount);
					fluidAmount -= handler.receiveFluid(fluidType, toSend);
				}
			} else if (te instanceof RetroNism_TileFluidPipe) {
				RetroNism_TileFluidPipe other = (RetroNism_TileFluidPipe) te;
				if (other.fluidAmount < this.fluidAmount) {
					int toSend = Math.min(perReceiver, fluidAmount);
					fluidAmount -= other.receiveFluid(fluidType, toSend);
				}
			}
		}

		if (fluidAmount == 0) fluidType = RetroNism_FluidType.NONE;
	}

	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		fluidType = nbt.getInteger("FluidType");
		fluidAmount = nbt.getInteger("FluidAmount");
		if (nbt.hasKey("SC0")) {
			for (int i = 0; i < 24; i++) this.sideConfig[i] = nbt.getInteger("SC" + i);
		}
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("FluidType", fluidType);
		nbt.setInteger("FluidAmount", fluidAmount);
		for (int i = 0; i < 24; i++) nbt.setInteger("SC" + i, this.sideConfig[i]);
	}
}
