package retronism.tile;

import net.minecraft.src.*;
import retronism.api.*;

public class RetroNism_TileElectrolysis extends TileEntity implements RetroNism_IEnergyReceiver, RetroNism_IFluidHandler, RetroNism_IGasHandler {
	public int storedEnergy = 0;
	public int waterStored = 0;
	public int hydrogenStored = 0;
	public int oxygenStored = 0;
	public int processTime = 0;

	public static final int MAX_ENERGY = 32000;
	public static final int MAX_WATER = 8000;
	public static final int MAX_HYDROGEN = 8000;
	public static final int MAX_OXYGEN = 8000;
	private static final int ENERGY_PER_TICK = 16;
	private static final int PROCESS_DURATION = 200;
	private static final int WATER_PER_OP = 1000;
	private static final int H2_PER_OP = 1000;
	private static final int O2_PER_OP = 500;
	private static final int GAS_PUSH_RATE = 200;

	// Energy
	public int receiveEnergy(int amount) {
		int space = MAX_ENERGY - storedEnergy;
		int accepted = Math.min(amount, space);
		storedEnergy += accepted;
		return accepted;
	}

	public int getStoredEnergy() { return storedEnergy; }
	public int getMaxEnergy() { return MAX_ENERGY; }

	// Fluid (accepts water input only)
	public int receiveFluid(int fluidType, int amountMB) {
		if (fluidType != RetroNism_FluidType.WATER) return 0;
		int space = MAX_WATER - waterStored;
		int accepted = Math.min(amountMB, space);
		waterStored += accepted;
		return accepted;
	}

	public int extractFluid(int fluidType, int amountMB) {
		return 0;
	}

	public int getFluidType() { return waterStored > 0 ? RetroNism_FluidType.WATER : RetroNism_FluidType.NONE; }
	public int getFluidAmount() { return waterStored; }
	public int getFluidCapacity() { return MAX_WATER; }

	// Gas (allows extraction of H2 or O2)
	public int receiveGas(int gasType, int amountMB) {
		return 0;
	}

	public int extractGas(int gasType, int amountMB) {
		if (gasType == RetroNism_GasType.HYDROGEN && hydrogenStored > 0) {
			int extracted = Math.min(amountMB, hydrogenStored);
			hydrogenStored -= extracted;
			return extracted;
		}
		if (gasType == RetroNism_GasType.OXYGEN && oxygenStored > 0) {
			int extracted = Math.min(amountMB, oxygenStored);
			oxygenStored -= extracted;
			return extracted;
		}
		return 0;
	}

	public int getGasType() {
		if (hydrogenStored > 0) return RetroNism_GasType.HYDROGEN;
		if (oxygenStored > 0) return RetroNism_GasType.OXYGEN;
		return RetroNism_GasType.NONE;
	}

	public int getGasAmount() { return hydrogenStored + oxygenStored; }
	public int getGasCapacity() { return MAX_HYDROGEN + MAX_OXYGEN; }

	// Scaled methods for GUI
	public int getEnergyScaled(int scale) { return storedEnergy * scale / MAX_ENERGY; }
	public int getWaterScaled(int scale) { return waterStored * scale / MAX_WATER; }
	public int getHydrogenScaled(int scale) { return hydrogenStored * scale / MAX_HYDROGEN; }
	public int getOxygenScaled(int scale) { return oxygenStored * scale / MAX_OXYGEN; }
	public int getProcessScaled(int scale) { return processTime * scale / PROCESS_DURATION; }

	public void updateEntity() {
		if (this.worldObj.multiplayerWorld) return;

		boolean changed = false;

		if (canProcess()) {
			storedEnergy -= ENERGY_PER_TICK;
			++processTime;
			changed = true;
			if (processTime >= PROCESS_DURATION) {
				processTime = 0;
				waterStored -= WATER_PER_OP;
				hydrogenStored += H2_PER_OP;
				oxygenStored += O2_PER_OP;
			}
		} else if (!canProcess()) {
			processTime = 0;
		}

		// Push gases to adjacent gas handlers
		if (hydrogenStored > 0 || oxygenStored > 0) {
			pushGasToNeighbors();
			changed = true;
		}

		if (changed) {
			this.onInventoryChanged();
		}
	}

	private boolean canProcess() {
		return waterStored >= WATER_PER_OP
			&& storedEnergy >= ENERGY_PER_TICK
			&& hydrogenStored + H2_PER_OP <= MAX_HYDROGEN
			&& oxygenStored + O2_PER_OP <= MAX_OXYGEN;
	}

	private void pushGasToNeighbors() {
		int[][] dirs = {{-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1}};

		for (int[] d : dirs) {
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te instanceof RetroNism_IGasHandler && te != this) {
				RetroNism_IGasHandler handler = (RetroNism_IGasHandler) te;
				// Try pushing hydrogen
				if (hydrogenStored > 0) {
					int toSend = Math.min(GAS_PUSH_RATE, hydrogenStored);
					int accepted = handler.receiveGas(RetroNism_GasType.HYDROGEN, toSend);
					hydrogenStored -= accepted;
				}
				// Try pushing oxygen
				if (oxygenStored > 0) {
					int toSend = Math.min(GAS_PUSH_RATE, oxygenStored);
					int accepted = handler.receiveGas(RetroNism_GasType.OXYGEN, toSend);
					oxygenStored -= accepted;
				}
			}
		}
	}

	public boolean canInteractWith(EntityPlayer player) {
		return this.worldObj.getBlockTileEntity(this.xCoord, this.yCoord, this.zCoord) == this
			&& player.getDistanceSq((double) this.xCoord + 0.5D, (double) this.yCoord + 0.5D, (double) this.zCoord + 0.5D) <= 64.0D;
	}

	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		storedEnergy = nbt.getInteger("Energy");
		waterStored = nbt.getInteger("WaterAmount");
		hydrogenStored = nbt.getInteger("HydrogenAmount");
		oxygenStored = nbt.getInteger("OxygenAmount");
		processTime = nbt.getShort("ProcessTime");
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("Energy", storedEnergy);
		nbt.setInteger("WaterAmount", waterStored);
		nbt.setInteger("HydrogenAmount", hydrogenStored);
		nbt.setInteger("OxygenAmount", oxygenStored);
		nbt.setShort("ProcessTime", (short) processTime);
	}
}
