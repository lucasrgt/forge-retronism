package retronism.tile;

import net.minecraft.src.*;
import retronism.api.*;

public class RetroNism_TilePump extends TileEntity implements RetroNism_IEnergyReceiver, RetroNism_IFluidHandler, IInventory {
	private ItemStack[] pumpItems = new ItemStack[1]; // bucket slot
	public int storedEnergy = 0;
	public int fluidAmount = 0;
	public static final int MAX_ENERGY = 16000;
	public static final int MAX_FLUID = 8000;
	private static final int ENERGY_PER_TICK = 16;
	private static final int FLUID_PER_TICK = 50;
	private static final int PUSH_RATE = 200;
	private static final int BUCKET_AMOUNT = 1000;

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
		if (fluidType != RetroNism_FluidType.WATER || fluidAmount <= 0) return 0;
		int extracted = Math.min(amountMB, fluidAmount);
		fluidAmount -= extracted;
		return extracted;
	}

	public int getFluidType() { return fluidAmount > 0 ? RetroNism_FluidType.WATER : RetroNism_FluidType.NONE; }
	public int getFluidAmount() { return fluidAmount; }
	public int getFluidCapacity() { return MAX_FLUID; }

	public int getEnergyScaled(int scale) {
		return storedEnergy * scale / MAX_ENERGY;
	}

	public int getFluidScaled(int scale) {
		return fluidAmount * scale / MAX_FLUID;
	}

	public void updateEntity() {
		if (this.worldObj.multiplayerWorld) return;

		boolean changed = false;

		// Pump water from below
		int belowID = worldObj.getBlockId(xCoord, yCoord - 1, zCoord);
		boolean aboveWater = belowID == Block.waterStill.blockID || belowID == Block.waterMoving.blockID;

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
		int[][] dirs = {{-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1}};

		for (int[] d : dirs) {
			if (fluidAmount <= 0) break;
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te instanceof RetroNism_IFluidHandler && te != this) {
				RetroNism_IFluidHandler handler = (RetroNism_IFluidHandler) te;
				int toSend = Math.min(PUSH_RATE, fluidAmount);
				int accepted = handler.receiveFluid(RetroNism_FluidType.WATER, toSend);
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
