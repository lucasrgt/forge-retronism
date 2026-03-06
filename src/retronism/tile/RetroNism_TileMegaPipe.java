package retronism.tile;

import net.minecraft.src.*;
import retronism.*;
import retronism.api.*;
import retronism.block.*;

public class Retronism_TileMegaPipe extends TileEntity
		implements Retronism_IEnergyReceiver, Retronism_IFluidHandler, Retronism_IGasHandler, Retronism_ISideConfigurable {

	// Energy buffer
	public static final int MAX_ENERGY = 800;
	public static final int ENERGY_TRANSFER = 200;
	public int storedEnergy = 0;
	private int energyReceivedThisTick = 0;

	// Fluid buffer
	public static final int MAX_FLUID = 500;
	public static final int FLUID_TRANSFER = 200;
	public int fluidAmount = 0;
	public int fluidType = Retronism_FluidType.NONE;
	private int fluidReceivedThisTick = 0;

	// Gas buffer
	public static final int MAX_GAS = 500;
	public static final int GAS_TRANSFER = 200;
	public int gasAmount = 0;
	public int gasType = Retronism_GasType.NONE;
	private int gasReceivedThisTick = 0;

	// Item buffer
	public ItemStack itemBuffer = null;

	// Side config
	private int[] sideConfig = new int[24];

	// Direction offsets: Bottom(0), Top(1), North(2), South(3), West(4), East(5)
	private static final int[][] DIRS = {
		{0,-1,0}, {0,1,0}, {0,0,-1}, {0,0,1}, {-1,0,0}, {1,0,0}
	};

	{
		for (int s = 0; s < 6; s++) {
			Retronism_SideConfig.set(sideConfig, s, Retronism_SideConfig.TYPE_ENERGY, Retronism_SideConfig.MODE_INPUT_OUTPUT);
			Retronism_SideConfig.set(sideConfig, s, Retronism_SideConfig.TYPE_FLUID, Retronism_SideConfig.MODE_INPUT_OUTPUT);
			Retronism_SideConfig.set(sideConfig, s, Retronism_SideConfig.TYPE_GAS, Retronism_SideConfig.MODE_INPUT_OUTPUT);
			Retronism_SideConfig.set(sideConfig, s, Retronism_SideConfig.TYPE_ITEM, Retronism_SideConfig.MODE_INPUT_OUTPUT);
		}
	}

	public int[] getSideConfig() { return sideConfig; }
	public void setSideMode(int side, int type, int mode) {
		Retronism_SideConfig.set(sideConfig, side, type, mode);
	}
	public boolean supportsType(int type) { return true; }

	private boolean canSendType(int side, TileEntity te, int type) {
		int myMode = Retronism_SideConfig.get(sideConfig, side, type);
		if (!Retronism_SideConfig.canOutput(myMode)) return false;
		int oppSide = Retronism_SideConfig.oppositeSide(side);
		if (te instanceof Retronism_ISideConfigurable) {
			int neighborMode = Retronism_SideConfig.get(((Retronism_ISideConfigurable) te).getSideConfig(), oppSide, type);
			if (!Retronism_SideConfig.canInput(neighborMode)) return false;
		}
		return true;
	}

	public void updateEntity() {
		if (this.worldObj.multiplayerWorld) return;

		this.energyReceivedThisTick = 0;
		this.fluidReceivedThisTick = 0;
		this.gasReceivedThisTick = 0;

		distributeEnergy();
		distributeFluid();
		distributeGas();
		distributeItems();
	}

	// ========== ENERGY ==========

	private void distributeEnergy() {
		if (this.storedEnergy <= 0) return;

		int receivers = 0;
		for (int side = 0; side < 6; side++) {
			int[] d = DIRS[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord+d[0], yCoord+d[1], zCoord+d[2]);
			if (te == null || te == this) continue;
			if (!canSendType(side, te, Retronism_SideConfig.TYPE_ENERGY)) continue;
			if (te instanceof Retronism_TileMegaPipe) {
				if (((Retronism_TileMegaPipe)te).storedEnergy < this.storedEnergy) receivers++;
			} else if (te instanceof Retronism_TileCable) {
				if (((Retronism_TileCable)te).getStoredEnergy() < this.storedEnergy) receivers++;
			} else if (te instanceof Retronism_IEnergyReceiver) {
				Retronism_IEnergyReceiver r = (Retronism_IEnergyReceiver) te;
				if (r.getStoredEnergy() < r.getMaxEnergy()) receivers++;
			}
		}
		if (receivers == 0) return;
		int perReceiver = Math.min(ENERGY_TRANSFER, this.storedEnergy) / receivers;
		if (perReceiver <= 0) return;

		for (int side = 0; side < 6; side++) {
			int[] d = DIRS[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord+d[0], yCoord+d[1], zCoord+d[2]);
			if (te == null || te == this) continue;
			if (!canSendType(side, te, Retronism_SideConfig.TYPE_ENERGY)) continue;
			if (te instanceof Retronism_TileMegaPipe) {
				Retronism_TileMegaPipe other = (Retronism_TileMegaPipe) te;
				if (other.storedEnergy < this.storedEnergy) {
					this.storedEnergy -= other.receiveEnergy(perReceiver);
				}
			} else if (te instanceof Retronism_TileCable) {
				Retronism_TileCable cable = (Retronism_TileCable) te;
				if (cable.getStoredEnergy() < this.storedEnergy) {
					this.storedEnergy -= cable.receiveEnergy(perReceiver);
				}
			} else if (te instanceof Retronism_IEnergyReceiver) {
				Retronism_IEnergyReceiver r = (Retronism_IEnergyReceiver) te;
				if (r.getStoredEnergy() < r.getMaxEnergy()) {
					this.storedEnergy -= r.receiveEnergy(perReceiver);
				}
			}
		}
	}

	public int receiveEnergy(int amount) {
		int canReceive = Math.min(amount, Math.min(ENERGY_TRANSFER - energyReceivedThisTick, MAX_ENERGY - storedEnergy));
		if (canReceive <= 0) return 0;
		this.storedEnergy += canReceive;
		this.energyReceivedThisTick += canReceive;
		return canReceive;
	}

	public int getStoredEnergy() { return this.storedEnergy; }
	public int getMaxEnergy() { return MAX_ENERGY; }

	// ========== FLUID ==========

	private void distributeFluid() {
		if (this.fluidAmount <= 0) return;

		int receivers = 0;
		for (int side = 0; side < 6; side++) {
			int[] d = DIRS[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord+d[0], yCoord+d[1], zCoord+d[2]);
			if (te == null || te == this) continue;
			if (!canSendType(side, te, Retronism_SideConfig.TYPE_FLUID)) continue;
			if (te instanceof Retronism_TileMegaPipe) {
				if (((Retronism_TileMegaPipe)te).fluidAmount < this.fluidAmount) receivers++;
			} else if (te instanceof Retronism_TileFluidPipe) {
				if (((Retronism_TileFluidPipe)te).getFluidAmount() < this.fluidAmount) receivers++;
			} else if (te instanceof Retronism_IFluidHandler) {
				Retronism_IFluidHandler h = (Retronism_IFluidHandler) te;
				if (h.getFluidAmount() < h.getFluidCapacity()) receivers++;
			}
		}
		if (receivers == 0) return;
		int perReceiver = Math.min(FLUID_TRANSFER, this.fluidAmount) / receivers;
		if (perReceiver <= 0) return;

		for (int side = 0; side < 6; side++) {
			int[] d = DIRS[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord+d[0], yCoord+d[1], zCoord+d[2]);
			if (te == null || te == this) continue;
			if (!canSendType(side, te, Retronism_SideConfig.TYPE_FLUID)) continue;
			if (te instanceof Retronism_TileMegaPipe) {
				Retronism_TileMegaPipe other = (Retronism_TileMegaPipe) te;
				if (other.fluidAmount < this.fluidAmount) {
					this.fluidAmount -= other.receiveFluid(this.fluidType, perReceiver);
				}
			} else if (te instanceof Retronism_TileFluidPipe) {
				Retronism_TileFluidPipe pipe = (Retronism_TileFluidPipe) te;
				if (pipe.getFluidAmount() < this.fluidAmount) {
					this.fluidAmount -= pipe.receiveFluid(this.fluidType, perReceiver);
				}
			} else if (te instanceof Retronism_IFluidHandler) {
				Retronism_IFluidHandler h = (Retronism_IFluidHandler) te;
				if (h.getFluidAmount() < h.getFluidCapacity()) {
					this.fluidAmount -= h.receiveFluid(this.fluidType, perReceiver);
				}
			}
		}
		if (this.fluidAmount <= 0) {
			this.fluidAmount = 0;
			this.fluidType = Retronism_FluidType.NONE;
		}
	}

	public int receiveFluid(int type, int amountMB) {
		if (this.fluidType != Retronism_FluidType.NONE && this.fluidType != type) return 0;
		int canReceive = Math.min(amountMB, Math.min(FLUID_TRANSFER - fluidReceivedThisTick, MAX_FLUID - fluidAmount));
		if (canReceive <= 0) return 0;
		this.fluidType = type;
		this.fluidAmount += canReceive;
		this.fluidReceivedThisTick += canReceive;
		return canReceive;
	}

	public int extractFluid(int type, int amountMB) {
		if (this.fluidType != type || this.fluidAmount <= 0) return 0;
		int extracted = Math.min(amountMB, this.fluidAmount);
		this.fluidAmount -= extracted;
		if (this.fluidAmount <= 0) { this.fluidAmount = 0; this.fluidType = Retronism_FluidType.NONE; }
		return extracted;
	}

	public int getFluidType() { return this.fluidType; }
	public int getFluidAmount() { return this.fluidAmount; }
	public int getFluidCapacity() { return MAX_FLUID; }

	// ========== GAS ==========

	private void distributeGas() {
		if (this.gasAmount <= 0) return;

		int receivers = 0;
		for (int side = 0; side < 6; side++) {
			int[] d = DIRS[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord+d[0], yCoord+d[1], zCoord+d[2]);
			if (te == null || te == this) continue;
			if (!canSendType(side, te, Retronism_SideConfig.TYPE_GAS)) continue;
			if (te instanceof Retronism_TileMegaPipe) {
				if (((Retronism_TileMegaPipe)te).gasAmount < this.gasAmount) receivers++;
			} else if (te instanceof Retronism_TileGasPipe) {
				if (((Retronism_TileGasPipe)te).getGasAmount() < this.gasAmount) receivers++;
			} else if (te instanceof Retronism_IGasHandler) {
				Retronism_IGasHandler h = (Retronism_IGasHandler) te;
				if (h.getGasAmount() < h.getGasCapacity()) receivers++;
			}
		}
		if (receivers == 0) return;
		int perReceiver = Math.min(GAS_TRANSFER, this.gasAmount) / receivers;
		if (perReceiver <= 0) return;

		for (int side = 0; side < 6; side++) {
			int[] d = DIRS[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord+d[0], yCoord+d[1], zCoord+d[2]);
			if (te == null || te == this) continue;
			if (!canSendType(side, te, Retronism_SideConfig.TYPE_GAS)) continue;
			if (te instanceof Retronism_TileMegaPipe) {
				Retronism_TileMegaPipe other = (Retronism_TileMegaPipe) te;
				if (other.gasAmount < this.gasAmount) {
					this.gasAmount -= other.receiveGas(this.gasType, perReceiver);
				}
			} else if (te instanceof Retronism_TileGasPipe) {
				Retronism_TileGasPipe pipe = (Retronism_TileGasPipe) te;
				if (pipe.getGasAmount() < this.gasAmount) {
					this.gasAmount -= pipe.receiveGas(this.gasType, perReceiver);
				}
			} else if (te instanceof Retronism_IGasHandler) {
				Retronism_IGasHandler h = (Retronism_IGasHandler) te;
				if (h.getGasAmount() < h.getGasCapacity()) {
					this.gasAmount -= h.receiveGas(this.gasType, perReceiver);
				}
			}
		}
		if (this.gasAmount <= 0) {
			this.gasAmount = 0;
			this.gasType = Retronism_GasType.NONE;
		}
	}

	public int receiveGas(int type, int amountMB) {
		if (this.gasType != Retronism_GasType.NONE && this.gasType != type) return 0;
		int canReceive = Math.min(amountMB, Math.min(GAS_TRANSFER - gasReceivedThisTick, MAX_GAS - gasAmount));
		if (canReceive <= 0) return 0;
		this.gasType = type;
		this.gasAmount += canReceive;
		this.gasReceivedThisTick += canReceive;
		return canReceive;
	}

	public int extractGas(int type, int amountMB) {
		if (this.gasType != type || this.gasAmount <= 0) return 0;
		int extracted = Math.min(amountMB, this.gasAmount);
		this.gasAmount -= extracted;
		if (this.gasAmount <= 0) { this.gasAmount = 0; this.gasType = Retronism_GasType.NONE; }
		return extracted;
	}

	public int getGasType() { return this.gasType; }
	public int getGasAmount() { return this.gasAmount; }
	public int getGasCapacity() { return MAX_GAS; }

	// ========== ITEMS ==========

	private void distributeItems() {
		if (this.itemBuffer == null) return;

		for (int side = 0; side < 6; side++) {
			int[] d = DIRS[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord+d[0], yCoord+d[1], zCoord+d[2]);
			if (te == null || te == this) continue;
			if (!canSendType(side, te, Retronism_SideConfig.TYPE_ITEM)) continue;
			if (te instanceof Retronism_TileMegaPipe) {
				Retronism_TileMegaPipe other = (Retronism_TileMegaPipe) te;
				if (other.itemBuffer == null) {
					other.itemBuffer = this.itemBuffer;
					this.itemBuffer = null;
					return;
				}
			} else if (te instanceof IInventory) {
				IInventory inv = (IInventory) te;
				this.itemBuffer = insertIntoInventory(inv, this.itemBuffer);
				if (this.itemBuffer != null && this.itemBuffer.stackSize <= 0) {
					this.itemBuffer = null;
				}
				if (this.itemBuffer == null) return;
			}
		}
	}

	private ItemStack insertIntoInventory(IInventory inv, ItemStack stack) {
		for (int i = 0; i < inv.getSizeInventory(); i++) {
			ItemStack existing = inv.getStackInSlot(i);
			if (existing == null) {
				inv.setInventorySlotContents(i, stack.copy());
				return null;
			} else if (existing.itemID == stack.itemID && existing.getItemDamage() == stack.getItemDamage()
					&& existing.stackSize < existing.getMaxStackSize()) {
				int space = existing.getMaxStackSize() - existing.stackSize;
				int transfer = Math.min(space, stack.stackSize);
				existing.stackSize += transfer;
				stack.stackSize -= transfer;
				if (stack.stackSize <= 0) return null;
			}
		}
		return stack;
	}

	// ========== NBT ==========

	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.storedEnergy = nbt.getInteger("Energy");
		this.fluidAmount = nbt.getInteger("FluidAmount");
		this.fluidType = nbt.getInteger("FluidType");
		this.gasAmount = nbt.getInteger("GasAmount");
		this.gasType = nbt.getInteger("GasType");
		if (nbt.hasKey("Item")) {
			this.itemBuffer = new ItemStack(nbt.getCompoundTag("Item"));
		}
		if (nbt.hasKey("SC0")) {
			for (int i = 0; i < 24; i++) this.sideConfig[i] = nbt.getInteger("SC" + i);
		}
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("Energy", this.storedEnergy);
		nbt.setInteger("FluidAmount", this.fluidAmount);
		nbt.setInteger("FluidType", this.fluidType);
		nbt.setInteger("GasAmount", this.gasAmount);
		nbt.setInteger("GasType", this.gasType);
		if (this.itemBuffer != null) {
			NBTTagCompound itemTag = new NBTTagCompound();
			this.itemBuffer.writeToNBT(itemTag);
			nbt.setCompoundTag("Item", itemTag);
		}
		for (int i = 0; i < 24; i++) nbt.setInteger("SC" + i, this.sideConfig[i]);
	}
}
