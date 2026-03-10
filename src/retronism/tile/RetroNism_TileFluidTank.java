package retronism.tile;

import net.minecraft.src.*;
import aero.machineapi.*;

public class Retronism_TileFluidTank extends TileEntity implements Aero_IFluidHandler, IInventory, Aero_ISideConfigurable, Aero_ISlotAccess {
	private ItemStack[] tankItems = new ItemStack[1];
	private int fluidAmount = 0;

	public static final int MAX_FLUID = 16000;
	private static final int BUCKET_AMOUNT = 1000;
	private int[] sideConfig = new int[24];

	{
		for (int s = 0; s < 6; s++) {
			Aero_SideConfig.set(sideConfig, s, Aero_SideConfig.TYPE_FLUID, Aero_SideConfig.MODE_INPUT_OUTPUT);
			Aero_SideConfig.set(sideConfig, s, Aero_SideConfig.TYPE_ITEM, Aero_SideConfig.MODE_INPUT_OUTPUT);
		}
	}

	public int[] getSideConfig() { return sideConfig; }
	public void setSideMode(int side, int type, int mode) {
		if (!supportsType(type)) return;
		int[] allowed = getAllowedModes(type);
		for (int m : allowed) { if (m == mode) { Aero_SideConfig.set(sideConfig, side, type, mode); return; } }
	}
	public boolean supportsType(int type) {
		return type == Aero_SideConfig.TYPE_FLUID || type == Aero_SideConfig.TYPE_ITEM;
	}
	public int[] getAllowedModes(int type) {
		if (type == Aero_SideConfig.TYPE_FLUID) return new int[]{Aero_SideConfig.MODE_NONE, Aero_SideConfig.MODE_INPUT, Aero_SideConfig.MODE_OUTPUT, Aero_SideConfig.MODE_INPUT_OUTPUT};
		if (type == Aero_SideConfig.TYPE_ITEM) return new int[]{Aero_SideConfig.MODE_NONE, Aero_SideConfig.MODE_INPUT, Aero_SideConfig.MODE_OUTPUT, Aero_SideConfig.MODE_INPUT_OUTPUT};
		return new int[]{Aero_SideConfig.MODE_NONE};
	}

	public int[] getInsertSlots() { return new int[]{0}; }
	public int[] getExtractSlots() { return new int[]{0}; }

	public int receiveFluid(int fluidType, int amountMB) {
		if (fluidType != Aero_FluidType.WATER) return 0;
		int space = MAX_FLUID - fluidAmount;
		int accepted = Math.min(amountMB, space);
		fluidAmount += accepted;
		return accepted;
	}

	public int extractFluid(int fluidType, int amountMB) {
		if (fluidType != Aero_FluidType.WATER || fluidAmount <= 0) return 0;
		int extracted = Math.min(amountMB, fluidAmount);
		fluidAmount -= extracted;
		return extracted;
	}

	public int getFluidType() {
		return fluidAmount > 0 ? Aero_FluidType.WATER : Aero_FluidType.NONE;
	}

	public int getFluidAmount() { return fluidAmount; }
	public int getFluidCapacity() { return MAX_FLUID; }

	public int getFluidScaled(int scale) {
		return fluidAmount * scale / MAX_FLUID;
	}

	public void setFluidAmountClient(int amount) {
		this.fluidAmount = amount;
	}

	public void updateEntity() {
		if (this.worldObj.multiplayerWorld) return;

		if (tankItems[0] != null && tankItems[0].itemID == Item.bucketEmpty.shiftedIndex && fluidAmount >= BUCKET_AMOUNT) {
			fluidAmount -= BUCKET_AMOUNT;
			tankItems[0] = new ItemStack(Item.bucketWater);
			this.onInventoryChanged();
		}
	}

	// IInventory
	public int getSizeInventory() { return this.tankItems.length; }
	public ItemStack getStackInSlot(int slot) { return this.tankItems[slot]; }

	public ItemStack decrStackSize(int slot, int amount) {
		if (this.tankItems[slot] != null) {
			ItemStack stack;
			if (this.tankItems[slot].stackSize <= amount) {
				stack = this.tankItems[slot];
				this.tankItems[slot] = null;
				return stack;
			} else {
				stack = this.tankItems[slot].splitStack(amount);
				if (this.tankItems[slot].stackSize == 0) this.tankItems[slot] = null;
				return stack;
			}
		}
		return null;
	}

	public void setInventorySlotContents(int slot, ItemStack stack) {
		this.tankItems[slot] = stack;
		if (stack != null && stack.stackSize > this.getInventoryStackLimit()) {
			stack.stackSize = this.getInventoryStackLimit();
		}
	}

	public String getInvName() { return "Fluid Tank"; }
	public int getInventoryStackLimit() { return 64; }

	public boolean canInteractWith(EntityPlayer player) {
		return this.worldObj.getBlockTileEntity(this.xCoord, this.yCoord, this.zCoord) == this
			&& player.getDistanceSq((double) this.xCoord + 0.5D, (double) this.yCoord + 0.5D, (double) this.zCoord + 0.5D) <= 64.0D;
	}

	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		fluidAmount = nbt.getInteger("FluidAmount");
		if (nbt.hasKey("SC0")) {
			for (int i = 0; i < 24; i++) this.sideConfig[i] = nbt.getInteger("SC" + i);
		}
		NBTTagList list = nbt.getTagList("Items");
		this.tankItems = new ItemStack[this.getSizeInventory()];
		for (int i = 0; i < list.tagCount(); ++i) {
			NBTTagCompound tag = (NBTTagCompound) list.tagAt(i);
			byte slot = tag.getByte("Slot");
			if (slot >= 0 && slot < this.tankItems.length) {
				this.tankItems[slot] = new ItemStack(tag);
			}
		}
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("FluidAmount", fluidAmount);
		for (int i = 0; i < 24; i++) nbt.setInteger("SC" + i, this.sideConfig[i]);
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < this.tankItems.length; ++i) {
			if (this.tankItems[i] != null) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setByte("Slot", (byte) i);
				this.tankItems[i].writeToNBT(tag);
				list.setTag(tag);
			}
		}
		nbt.setTag("Items", list);
	}
}
