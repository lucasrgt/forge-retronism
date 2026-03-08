package retronism.tile;

import net.minecraft.src.*;
import retronism.*;
import retronism.api.*;

public class Retronism_TileMegaElectrolysis extends TileEntity implements Retronism_IEnergyReceiver, Retronism_IFluidHandler, Retronism_IGasHandler, Retronism_ISideConfigurable {

	public boolean isFormed = false;
	public int structOffX, structOffY, structOffZ;

	public int storedEnergy = 0;
	public int waterStored = 0;
	public int hydrogenStored = 0;
	public int oxygenStored = 0;
	public int heavyWaterStored = 0;
	public int processTime = 0;
	private int[] sideConfig = new int[24];
	private int validationTimer = 0;

	public static final int MAX_ENERGY = 64000;
	public static final int MAX_WATER = 16000;
	public static final int MAX_HYDROGEN = 16000;
	public static final int MAX_OXYGEN = 16000;
	public static final int MAX_HEAVY_WATER = 8000;
	private static final int ENERGY_PER_TICK = 32;
	private static final int PROCESS_DURATION = 300;
	private static final int WATER_PER_OP = 2000;
	private static final int H2_PER_OP = 1000;
	private static final int O2_PER_OP = 500;
	private static final int HW_PER_OP = 500;
	private static final int GAS_PUSH_RATE = 200;
	private static final int FLUID_PUSH_RATE = 200;

	{
		for (int s = 0; s < 6; s++) {
			Retronism_SideConfig.set(sideConfig, s, Retronism_SideConfig.TYPE_ENERGY, Retronism_SideConfig.MODE_INPUT);
			Retronism_SideConfig.set(sideConfig, s, Retronism_SideConfig.TYPE_FLUID, Retronism_SideConfig.MODE_INPUT_OUTPUT);
			Retronism_SideConfig.set(sideConfig, s, Retronism_SideConfig.TYPE_GAS, Retronism_SideConfig.MODE_OUTPUT);
		}
	}

	public int[] getSideConfig() { return sideConfig; }
	public void setSideMode(int side, int type, int mode) {
		if (!supportsType(type)) return;
		int[] allowed = getAllowedModes(type);
		for (int m : allowed) { if (m == mode) { Retronism_SideConfig.set(sideConfig, side, type, mode); return; } }
	}
	public boolean supportsType(int type) {
		return type == Retronism_SideConfig.TYPE_ENERGY || type == Retronism_SideConfig.TYPE_FLUID || type == Retronism_SideConfig.TYPE_GAS;
	}
	public int[] getAllowedModes(int type) {
		if (type == Retronism_SideConfig.TYPE_ENERGY) return new int[]{Retronism_SideConfig.MODE_NONE, Retronism_SideConfig.MODE_INPUT};
		if (type == Retronism_SideConfig.TYPE_FLUID) return new int[]{Retronism_SideConfig.MODE_NONE, Retronism_SideConfig.MODE_INPUT, Retronism_SideConfig.MODE_OUTPUT, Retronism_SideConfig.MODE_INPUT_OUTPUT};
		if (type == Retronism_SideConfig.TYPE_GAS) return new int[]{Retronism_SideConfig.MODE_NONE, Retronism_SideConfig.MODE_OUTPUT};
		return new int[]{Retronism_SideConfig.MODE_NONE};
	}

	// --- Structure ---
	private static final int TYPE_AIR = 0;
	private static final int TYPE_CASING = 1;
	private static final int TYPE_FLUID_PORT = 2;
	private static final int TYPE_CONTROLLER = 3;
	private static final int TYPE_GAS_PORT = 4;
	private static final int TYPE_ENERGY_PORT = 5;

	private static final int[][][] STRUCTURE = {
		{ // Layer 0
			{1, 1, 1},
			{1, 2, 1},
			{1, 1, 1},
		},
		{ // Layer 1
			{1, 3, 1},
			{2, 0, 2},
			{4, 4, 4},
		},
		{ // Layer 2
			{1, 1, 1},
			{5, 5, 5},
			{1, 1, 1},
		},
	};

	public boolean checkStructure(World world, int cx, int cy, int cz) {
		int ctrlX = -1, ctrlY = -1, ctrlZ = -1;
		for (int y = 0; y < STRUCTURE.length; y++) {
			for (int z = 0; z < STRUCTURE[y].length; z++) {
				for (int x = 0; x < STRUCTURE[y][z].length; x++) {
					if (STRUCTURE[y][z][x] == TYPE_CONTROLLER) {
						ctrlX = x; ctrlY = y; ctrlZ = z;
					}
				}
			}
		}
		if (ctrlX == -1) { isFormed = false; return false; }

		int casingId = Retronism_Registry.megaElectrolysisCasing.blockID;
		int controllerId = Retronism_Registry.megaElectrolysisController.blockID;

		for (int y = 0; y < STRUCTURE.length; y++) {
			for (int z = 0; z < STRUCTURE[y].length; z++) {
				for (int x = 0; x < STRUCTURE[y][z].length; x++) {
					int expected = STRUCTURE[y][z][x];
					if (expected == TYPE_AIR) continue;

					int wx = cx + (x - ctrlX);
					int wy = cy + (y - ctrlY);
					int wz = cz + (z - ctrlZ);
					int blockId = world.getBlockId(wx, wy, wz);

					if (expected == TYPE_CONTROLLER) {
						if (blockId != controllerId) { isFormed = false; return false; }
					} else {
						if (blockId != casingId && blockId != controllerId) {
							isFormed = false;
							return false;
						}
					}
				}
			}
		}
		isFormed = true;
		structOffX = ctrlX;
		structOffY = ctrlY;
		structOffZ = ctrlZ;
		return true;
	}

	// --- Energy ---
	public int receiveEnergy(int amount) {
		int space = MAX_ENERGY - storedEnergy;
		int accepted = Math.min(amount, space);
		storedEnergy += accepted;
		return accepted;
	}
	public int getStoredEnergy() { return storedEnergy; }
	public int getMaxEnergy() { return MAX_ENERGY; }

	// --- Fluid (water input + heavy water output) ---
	public int receiveFluid(int fluidType, int amountMB) {
		if (fluidType != Retronism_FluidType.WATER) return 0;
		int space = MAX_WATER - waterStored;
		int accepted = Math.min(amountMB, space);
		waterStored += accepted;
		return accepted;
	}

	public int extractFluid(int fluidType, int amountMB) {
		if (fluidType != Retronism_FluidType.HEAVY_WATER) return 0;
		if (heavyWaterStored <= 0) return 0;
		int extracted = Math.min(amountMB, heavyWaterStored);
		heavyWaterStored -= extracted;
		return extracted;
	}

	public int getFluidType() {
		if (heavyWaterStored > 0) return Retronism_FluidType.HEAVY_WATER;
		if (waterStored > 0) return Retronism_FluidType.WATER;
		return Retronism_FluidType.NONE;
	}
	public int getFluidAmount() { return waterStored + heavyWaterStored; }
	public int getFluidCapacity() { return MAX_WATER + MAX_HEAVY_WATER; }

	// --- Gas (H2 + O2 output) ---
	public int receiveGas(int gasType, int amountMB) {
		return 0;
	}

	public int extractGas(int gasType, int amountMB) {
		if (gasType == Retronism_GasType.HYDROGEN && hydrogenStored > 0) {
			int extracted = Math.min(amountMB, hydrogenStored);
			hydrogenStored -= extracted;
			return extracted;
		}
		if (gasType == Retronism_GasType.OXYGEN && oxygenStored > 0) {
			int extracted = Math.min(amountMB, oxygenStored);
			oxygenStored -= extracted;
			return extracted;
		}
		return 0;
	}

	public int getGasType() {
		if (hydrogenStored > 0) return Retronism_GasType.HYDROGEN;
		if (oxygenStored > 0) return Retronism_GasType.OXYGEN;
		return Retronism_GasType.NONE;
	}
	public int getGasAmount() { return hydrogenStored + oxygenStored; }
	public int getGasCapacity() { return MAX_HYDROGEN + MAX_OXYGEN; }

	// --- Scaled for GUI ---
	public int getEnergyScaled(int scale) { return storedEnergy * scale / MAX_ENERGY; }
	public int getWaterScaled(int scale) { return waterStored * scale / MAX_WATER; }
	public int getHydrogenScaled(int scale) { return hydrogenStored * scale / MAX_HYDROGEN; }
	public int getOxygenScaled(int scale) { return oxygenStored * scale / MAX_OXYGEN; }
	public int getHeavyWaterScaled(int scale) { return heavyWaterStored * scale / MAX_HEAVY_WATER; }
	public int getCookProgressScaled(int scale) { return processTime * scale / PROCESS_DURATION; }

	// --- Processing ---
	public void updateEntity() {
		if (worldObj.multiplayerWorld) return;
		if (!isFormed) return;

		validationTimer++;
		if (validationTimer >= 20) {
			validationTimer = 0;
			if (!checkStructure(worldObj, xCoord, yCoord, zCoord)) {
				isFormed = false;
				processTime = 0;
				return;
			}
		}

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
				heavyWaterStored += HW_PER_OP;
			}
		} else if (!canProcess()) {
			processTime = 0;
		}

		if (hydrogenStored > 0 || oxygenStored > 0) {
			pushGasToNeighbors();
			changed = true;
		}

		if (heavyWaterStored > 0) {
			pushHeavyWaterToNeighbors();
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
			&& oxygenStored + O2_PER_OP <= MAX_OXYGEN
			&& heavyWaterStored + HW_PER_OP <= MAX_HEAVY_WATER;
	}

	private void pushGasToNeighbors() {
		int[][] dirs = {{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
		for (int side = 0; side < 6; side++) {
			int myMode = Retronism_SideConfig.get(sideConfig, side, Retronism_SideConfig.TYPE_GAS);
			if (!Retronism_SideConfig.canOutput(myMode)) continue;
			int[] d = dirs[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te instanceof Retronism_IGasHandler && te != this) {
				int oppSide = Retronism_SideConfig.oppositeSide(side);
				if (te instanceof Retronism_ISideConfigurable) {
					int neighborMode = Retronism_SideConfig.get(((Retronism_ISideConfigurable) te).getSideConfig(), oppSide, Retronism_SideConfig.TYPE_GAS);
					if (!Retronism_SideConfig.canInput(neighborMode)) continue;
				}
				Retronism_IGasHandler handler = (Retronism_IGasHandler) te;
				if (hydrogenStored > 0) {
					int toSend = Math.min(GAS_PUSH_RATE, hydrogenStored);
					hydrogenStored -= handler.receiveGas(Retronism_GasType.HYDROGEN, toSend);
				}
				if (oxygenStored > 0) {
					int toSend = Math.min(GAS_PUSH_RATE, oxygenStored);
					oxygenStored -= handler.receiveGas(Retronism_GasType.OXYGEN, toSend);
				}
			}
		}
	}

	private void pushHeavyWaterToNeighbors() {
		int[][] dirs = {{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
		for (int side = 0; side < 6; side++) {
			int myMode = Retronism_SideConfig.get(sideConfig, side, Retronism_SideConfig.TYPE_FLUID);
			if (!Retronism_SideConfig.canOutput(myMode)) continue;
			int[] d = dirs[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te instanceof Retronism_IFluidHandler && te != this) {
				int oppSide = Retronism_SideConfig.oppositeSide(side);
				if (te instanceof Retronism_ISideConfigurable) {
					int neighborMode = Retronism_SideConfig.get(((Retronism_ISideConfigurable) te).getSideConfig(), oppSide, Retronism_SideConfig.TYPE_FLUID);
					if (!Retronism_SideConfig.canInput(neighborMode)) continue;
				}
				Retronism_IFluidHandler handler = (Retronism_IFluidHandler) te;
				int toSend = Math.min(FLUID_PUSH_RATE, heavyWaterStored);
				heavyWaterStored -= handler.receiveFluid(Retronism_FluidType.HEAVY_WATER, toSend);
			}
		}
	}

	public boolean canInteractWith(EntityPlayer player) {
		return this.worldObj.getBlockTileEntity(this.xCoord, this.yCoord, this.zCoord) == this
			&& player.getDistanceSq((double) this.xCoord + 0.5D, (double) this.yCoord + 0.5D, (double) this.zCoord + 0.5D) <= 64.0D;
	}

	// --- NBT ---
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		isFormed = nbt.getBoolean("Formed");
		structOffX = nbt.getInteger("StructOffX");
		structOffY = nbt.getInteger("StructOffY");
		structOffZ = nbt.getInteger("StructOffZ");
		storedEnergy = nbt.getInteger("Energy");
		waterStored = nbt.getInteger("WaterAmount");
		hydrogenStored = nbt.getInteger("HydrogenAmount");
		oxygenStored = nbt.getInteger("OxygenAmount");
		heavyWaterStored = nbt.getInteger("HeavyWaterAmount");
		processTime = nbt.getShort("ProcessTime");
		if (nbt.hasKey("SC0")) {
			for (int i = 0; i < 24; i++) this.sideConfig[i] = nbt.getInteger("SC" + i);
		}
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setBoolean("Formed", isFormed);
		nbt.setInteger("StructOffX", structOffX);
		nbt.setInteger("StructOffY", structOffY);
		nbt.setInteger("StructOffZ", structOffZ);
		nbt.setInteger("Energy", storedEnergy);
		nbt.setInteger("WaterAmount", waterStored);
		nbt.setInteger("HydrogenAmount", hydrogenStored);
		nbt.setInteger("OxygenAmount", oxygenStored);
		nbt.setInteger("HeavyWaterAmount", heavyWaterStored);
		nbt.setShort("ProcessTime", (short) processTime);
		for (int i = 0; i < 24; i++) nbt.setInteger("SC" + i, this.sideConfig[i]);
	}
}
