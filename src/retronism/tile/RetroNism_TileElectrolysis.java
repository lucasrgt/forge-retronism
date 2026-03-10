package retronism.tile;

import net.minecraft.src.*;
import aero.machineapi.*;

public class Retronism_TileElectrolysis extends TileEntity implements Aero_IEnergyReceiver, Aero_IFluidHandler, Aero_IGasHandler, Aero_ISideConfigurable {
	public int storedEnergy = 0;
	public int waterStored = 0;
	public int hydrogenStored = 0;
	public int oxygenStored = 0;
	public int processTime = 0;
	private int[] sideConfig = new int[24];

	{
		for (int s = 0; s < 6; s++) {
			Aero_SideConfig.set(sideConfig, s, Aero_SideConfig.TYPE_ENERGY, Aero_SideConfig.MODE_INPUT);
			Aero_SideConfig.set(sideConfig, s, Aero_SideConfig.TYPE_FLUID, Aero_SideConfig.MODE_INPUT);
			Aero_SideConfig.set(sideConfig, s, Aero_SideConfig.TYPE_GAS, Aero_SideConfig.MODE_OUTPUT);
		}
	}

	public int[] getSideConfig() { return sideConfig; }
	public void setSideMode(int side, int type, int mode) {
		if (!supportsType(type)) return;
		int[] allowed = getAllowedModes(type);
		for (int m : allowed) { if (m == mode) { Aero_SideConfig.set(sideConfig, side, type, mode); return; } }
	}
	public boolean supportsType(int type) {
		return type == Aero_SideConfig.TYPE_ENERGY || type == Aero_SideConfig.TYPE_FLUID || type == Aero_SideConfig.TYPE_GAS;
	}
	public int[] getAllowedModes(int type) {
		if (type == Aero_SideConfig.TYPE_ENERGY) return new int[]{Aero_SideConfig.MODE_NONE, Aero_SideConfig.MODE_INPUT};
		if (type == Aero_SideConfig.TYPE_FLUID) return new int[]{Aero_SideConfig.MODE_NONE, Aero_SideConfig.MODE_INPUT};
		if (type == Aero_SideConfig.TYPE_GAS) return new int[]{Aero_SideConfig.MODE_NONE, Aero_SideConfig.MODE_OUTPUT};
		return new int[]{Aero_SideConfig.MODE_NONE};
	}

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
		if (fluidType != Aero_FluidType.WATER) return 0;
		int space = MAX_WATER - waterStored;
		int accepted = Math.min(amountMB, space);
		waterStored += accepted;
		return accepted;
	}

	public int extractFluid(int fluidType, int amountMB) {
		return 0;
	}

	public int getFluidType() { return waterStored > 0 ? Aero_FluidType.WATER : Aero_FluidType.NONE; }
	public int getFluidAmount() { return waterStored; }
	public int getFluidCapacity() { return MAX_WATER; }

	// Gas (allows extraction of H2 or O2)
	public int receiveGas(int gasType, int amountMB) {
		return 0;
	}

	public int extractGas(int gasType, int amountMB) {
		if (gasType == Aero_GasType.HYDROGEN && hydrogenStored > 0) {
			int extracted = Math.min(amountMB, hydrogenStored);
			hydrogenStored -= extracted;
			return extracted;
		}
		if (gasType == Aero_GasType.OXYGEN && oxygenStored > 0) {
			int extracted = Math.min(amountMB, oxygenStored);
			oxygenStored -= extracted;
			return extracted;
		}
		return 0;
	}

	public int getGasType() {
		if (hydrogenStored > 0) return Aero_GasType.HYDROGEN;
		if (oxygenStored > 0) return Aero_GasType.OXYGEN;
		return Aero_GasType.NONE;
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
		int[][] dirs = {{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};

		for (int side = 0; side < 6; side++) {
			int myMode = Aero_SideConfig.get(sideConfig, side, Aero_SideConfig.TYPE_GAS);
			if (!Aero_SideConfig.canOutput(myMode)) continue;
			int[] d = dirs[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te instanceof Aero_IGasHandler && te != this) {
				int oppSide = Aero_SideConfig.oppositeSide(side);
				if (te instanceof Aero_ISideConfigurable) {
					int neighborMode = Aero_SideConfig.get(((Aero_ISideConfigurable) te).getSideConfig(), oppSide, Aero_SideConfig.TYPE_GAS);
					if (!Aero_SideConfig.canInput(neighborMode)) continue;
				}
				Aero_IGasHandler handler = (Aero_IGasHandler) te;
				if (hydrogenStored > 0) {
					int toSend = Math.min(GAS_PUSH_RATE, hydrogenStored);
					hydrogenStored -= handler.receiveGas(Aero_GasType.HYDROGEN, toSend);
				}
				if (oxygenStored > 0) {
					int toSend = Math.min(GAS_PUSH_RATE, oxygenStored);
					oxygenStored -= handler.receiveGas(Aero_GasType.OXYGEN, toSend);
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
		if (nbt.hasKey("SC0")) {
			for (int i = 0; i < 24; i++) this.sideConfig[i] = nbt.getInteger("SC" + i);
		}
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("Energy", storedEnergy);
		nbt.setInteger("WaterAmount", waterStored);
		nbt.setInteger("HydrogenAmount", hydrogenStored);
		nbt.setInteger("OxygenAmount", oxygenStored);
		nbt.setShort("ProcessTime", (short) processTime);
		for (int i = 0; i < 24; i++) nbt.setInteger("SC" + i, this.sideConfig[i]);
	}
}
