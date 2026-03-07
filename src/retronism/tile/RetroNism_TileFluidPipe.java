package retronism.tile;

import net.minecraft.src.*;
import retronism.api.*;

public class Retronism_TileFluidPipe extends TileEntity implements Retronism_IFluidHandler, Retronism_ISideConfigurable {
	private int fluidType = Retronism_FluidType.NONE;
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
			Retronism_SideConfig.set(sideConfig, s, Retronism_SideConfig.TYPE_FLUID, Retronism_SideConfig.MODE_INPUT_OUTPUT);
		}
	}

	public int[] getSideConfig() { return sideConfig; }
	public void setSideMode(int side, int type, int mode) {
		if (!supportsType(type)) return;
		int[] allowed = getAllowedModes(type);
		for (int m : allowed) { if (m == mode) { Retronism_SideConfig.set(sideConfig, side, type, mode); return; } }
	}
	public boolean supportsType(int type) {
		return type == Retronism_SideConfig.TYPE_FLUID;
	}
	public int[] getAllowedModes(int type) {
		if (type == Retronism_SideConfig.TYPE_FLUID) return new int[]{Retronism_SideConfig.MODE_NONE, Retronism_SideConfig.MODE_INPUT, Retronism_SideConfig.MODE_OUTPUT, Retronism_SideConfig.MODE_INPUT_OUTPUT};
		return new int[]{Retronism_SideConfig.MODE_NONE};
	}

	public int receiveFluid(int type, int amountMB) {
		if (type == Retronism_FluidType.NONE) return 0;
		if (fluidType != Retronism_FluidType.NONE && fluidType != type) return 0;
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
		if (fluidAmount == 0) fluidType = Retronism_FluidType.NONE;
		return extracted;
	}

	public int getFluidType() { return fluidType; }
	public int getFluidAmount() { return fluidAmount; }
	public int getFluidCapacity() { return MAX_FLUID; }

	public int getSideMode(int side) {
		return Retronism_SideConfig.get(sideConfig, side, Retronism_SideConfig.TYPE_FLUID);
	}

	private boolean canSendTo(int side, TileEntity te) {
		if (!Retronism_SideConfig.canOutput(getSideMode(side))) return false;
		int oppSide = Retronism_SideConfig.oppositeSide(side);
		if (te instanceof Retronism_ISideConfigurable) {
			int neighborMode = Retronism_SideConfig.get(((Retronism_ISideConfigurable) te).getSideConfig(), oppSide, Retronism_SideConfig.TYPE_FLUID);
			if (!Retronism_SideConfig.canInput(neighborMode)) return false;
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
			if (te instanceof Retronism_IFluidHandler && !(te instanceof Retronism_TileFluidPipe)) {
				if (((Retronism_IFluidHandler) te).getFluidAmount() < ((Retronism_IFluidHandler) te).getFluidCapacity()) receivers++;
			} else if (te instanceof Retronism_TileFluidPipe) {
				if (((Retronism_TileFluidPipe) te).fluidAmount < this.fluidAmount) receivers++;
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
			if (te instanceof Retronism_IFluidHandler && !(te instanceof Retronism_TileFluidPipe)) {
				Retronism_IFluidHandler handler = (Retronism_IFluidHandler) te;
				if (handler.getFluidAmount() < handler.getFluidCapacity()) {
					int toSend = Math.min(perReceiver, fluidAmount);
					fluidAmount -= handler.receiveFluid(fluidType, toSend);
				}
			} else if (te instanceof Retronism_TileFluidPipe) {
				Retronism_TileFluidPipe other = (Retronism_TileFluidPipe) te;
				if (other.fluidAmount < this.fluidAmount) {
					int toSend = Math.min(perReceiver, fluidAmount);
					fluidAmount -= other.receiveFluid(fluidType, toSend);
				}
			}
		}

		if (fluidAmount == 0) fluidType = Retronism_FluidType.NONE;
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
