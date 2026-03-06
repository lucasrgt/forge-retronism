package retronism.tile;

import net.minecraft.src.*;
import retronism.api.*;

public class Retronism_TilePump extends TileEntity implements Retronism_IEnergyReceiver, Retronism_IFluidHandler, IInventory, Retronism_ISideConfigurable {
	private ItemStack[] pumpItems = new ItemStack[1]; // bucket slot
	public int storedEnergy = 0;
	public int fluidAmount = 0;
	public static final int MAX_ENERGY = 16000;
	public static final int MAX_FLUID = 8000;
	private static final int ENERGY_PER_TICK = 16;
	private static final int FLUID_PER_TICK = 50;
	private static final int PUSH_RATE = 200;
	private static final int BUCKET_AMOUNT = 1000;
	private static final int WATER_SOURCE_BLOCK_ID = 9; // Beta 1.7.3: still water
	private int[] sideConfig = new int[24];

	{
		for (int s = 0; s < 6; s++) {
			Retronism_SideConfig.set(sideConfig, s, Retronism_SideConfig.TYPE_ENERGY, Retronism_SideConfig.MODE_INPUT);
			Retronism_SideConfig.set(sideConfig, s, Retronism_SideConfig.TYPE_FLUID, Retronism_SideConfig.MODE_OUTPUT);
			Retronism_SideConfig.set(sideConfig, s, Retronism_SideConfig.TYPE_ITEM, Retronism_SideConfig.MODE_INPUT_OUTPUT);
		}
	}

	public int[] getSideConfig() { return sideConfig; }
	public void setSideMode(int side, int type, int mode) {
		if (supportsType(type)) Retronism_SideConfig.set(sideConfig, side, type, mode);
	}
	public boolean supportsType(int type) {
		return type == Retronism_SideConfig.TYPE_ENERGY || type == Retronism_SideConfig.TYPE_FLUID || type == Retronism_SideConfig.TYPE_ITEM;
	}

	public int receiveEnergy(int amount) {
		int space = MAX_ENERGY - storedEnergy;
		int accepted = Math.min(amount, space);
		storedEnergy += accepted;
		return accepted;
	}

	public int getStoredEnergy() { return storedEnergy; }
	public int getMaxEnergy() { return MAX_ENERGY; }

	public int receiveFluid(int fluidType, int amountMB) {
		return 0;
	}

	public int extractFluid(int fluidType, int amountMB) {
		if (fluidType != Retronism_FluidType.WATER || fluidAmount <= 0) return 0;
		int extracted = Math.min(amountMB, fluidAmount);
		fluidAmount -= extracted;
		return extracted;
	}

	public int getFluidType() { return fluidAmount > 0 ? Retronism_FluidType.WATER : Retronism_FluidType.NONE; }
	public int getFluidAmount() { return fluidAmount; }
	public int getFluidCapacity() { return MAX_FLUID; }

	public int getEnergyScaled(int scale) {
		return storedEnergy * scale / MAX_ENERGY;
	}

	public int getFluidScaled(int scale) {
		return fluidAmount * scale / MAX_FLUID;
	}

	public static boolean isPumpableWaterBlock(int blockID) {
		return blockID == WATER_SOURCE_BLOCK_ID;
	}

	public void updateEntity() {
		if (this.worldObj.multiplayerWorld) return;

		boolean changed = false;

		// Pump water from below
		int belowID = worldObj.getBlockId(xCoord, yCoord - 1, zCoord);
		boolean aboveWater = isPumpableWaterBlock(belowID);

		if (aboveWater && storedEnergy >= ENERGY_PER_TICK && fluidAmount + FLUID_PER_TICK <= MAX_FLUID) {
			storedEnergy -= ENERGY_PER_TICK;
			fluidAmount += FLUID_PER_TICK;
			changed = true;
		}

		// Fill bucket: empty bucket + 1000 mB water -> water bucket
		if (pumpItems[0] != null && pumpItems[0].itemID == Item.bucketEmpty.shiftedIndex && fluidAmount >= BUCKET_AMOUNT) {
			fluidAmount -= BUCKET_AMOUNT;
			pumpItems[0] = new ItemStack(Item.bucketWater);
			changed = true;
		}

		// Push fluid to neighbors
		if (fluidAmount > 0) {
			pushFluidToNeighbors();
			changed = true;
		}

		if (changed) {
			this.onInventoryChanged();
		}
	}

	private void pushFluidToNeighbors() {
		int[][] dirs = {{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};

		for (int side = 0; side < 6; side++) {
			if (fluidAmount <= 0) break;
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
				int toSend = Math.min(PUSH_RATE, fluidAmount);
				int accepted = handler.receiveFluid(Retronism_FluidType.WATER, toSend);
				fluidAmount -= accepted;
			}
		}
	}

	// IInventory
	public int getSizeInventory() { return this.pumpItems.length; }
	public ItemStack getStackInSlot(int slot) { return this.pumpItems[slot]; }

	public ItemStack decrStackSize(int slot, int amount) {
		if (this.pumpItems[slot] != null) {
			ItemStack stack;
			if (this.pumpItems[slot].stackSize <= amount) {
				stack = this.pumpItems[slot];
				this.pumpItems[slot] = null;
				return stack;
			} else {
				stack = this.pumpItems[slot].splitStack(amount);
				if (this.pumpItems[slot].stackSize == 0) {
					this.pumpItems[slot] = null;
				}
				return stack;
			}
		}
		return null;
	}

	public void setInventorySlotContents(int slot, ItemStack stack) {
		this.pumpItems[slot] = stack;
		if (stack != null && stack.stackSize > this.getInventoryStackLimit()) {
			stack.stackSize = this.getInventoryStackLimit();
		}
	}

	public String getInvName() { return "Water Pump"; }
	public int getInventoryStackLimit() { return 64; }

	public boolean canInteractWith(EntityPlayer player) {
		return this.worldObj.getBlockTileEntity(this.xCoord, this.yCoord, this.zCoord) == this
			&& player.getDistanceSq((double) this.xCoord + 0.5D, (double) this.yCoord + 0.5D, (double) this.zCoord + 0.5D) <= 64.0D;
	}

	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		storedEnergy = nbt.getInteger("Energy");
		fluidAmount = nbt.getInteger("FluidAmount");
		if (nbt.hasKey("SC0")) {
			for (int i = 0; i < 24; i++) this.sideConfig[i] = nbt.getInteger("SC" + i);
		}
		NBTTagList list = nbt.getTagList("Items");
		this.pumpItems = new ItemStack[this.getSizeInventory()];
		for (int i = 0; i < list.tagCount(); ++i) {
			NBTTagCompound tag = (NBTTagCompound) list.tagAt(i);
			byte slot = tag.getByte("Slot");
			if (slot >= 0 && slot < this.pumpItems.length) {
				this.pumpItems[slot] = new ItemStack(tag);
			}
		}
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("Energy", storedEnergy);
		nbt.setInteger("FluidAmount", fluidAmount);
		for (int i = 0; i < 24; i++) nbt.setInteger("SC" + i, this.sideConfig[i]);
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < this.pumpItems.length; ++i) {
			if (this.pumpItems[i] != null) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setByte("Slot", (byte) i);
				this.pumpItems[i].writeToNBT(tag);
				list.setTag(tag);
			}
		}
		nbt.setTag("Items", list);
	}
}
