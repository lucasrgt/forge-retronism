package retronism.tile;

import net.minecraft.src.*;
import aero.machineapi.*;
import retronism.recipe.*;

public class Retronism_TileCrusher extends TileEntity implements IInventory, Aero_IEnergyReceiver, Aero_ISideConfigurable, Aero_ISlotAccess {
	private ItemStack[] crusherItemStacks = new ItemStack[2]; // 0=input, 1=output
	public int crusherCookTime = 0;
	public int storedEnergy = 0;
	public static final int MAX_ENERGY = 32000;
	private static final int ENERGY_PER_TICK = 8;
	private int[] sideConfig = new int[24];

	{
		for (int s = 0; s < 6; s++) {
			Aero_SideConfig.set(sideConfig, s, Aero_SideConfig.TYPE_ENERGY, Aero_SideConfig.MODE_INPUT);
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
		return type == Aero_SideConfig.TYPE_ENERGY || type == Aero_SideConfig.TYPE_ITEM;
	}
	public int[] getAllowedModes(int type) {
		if (type == Aero_SideConfig.TYPE_ENERGY) return new int[]{Aero_SideConfig.MODE_NONE, Aero_SideConfig.MODE_INPUT};
		if (type == Aero_SideConfig.TYPE_ITEM) return new int[]{Aero_SideConfig.MODE_NONE, Aero_SideConfig.MODE_INPUT, Aero_SideConfig.MODE_OUTPUT, Aero_SideConfig.MODE_INPUT_OUTPUT};
		return new int[]{Aero_SideConfig.MODE_NONE};
	}

	public int[] getInsertSlots() { return new int[]{0}; }
	public int[] getExtractSlots() { return new int[]{1}; }

	public int receiveEnergy(int amount) {
		int space = MAX_ENERGY - storedEnergy;
		int accepted = Math.min(amount, space);
		storedEnergy += accepted;
		return accepted;
	}

	public int getStoredEnergy() {
		return storedEnergy;
	}

	public int getMaxEnergy() {
		return MAX_ENERGY;
	}

	public int getSizeInventory() {
		return this.crusherItemStacks.length;
	}

	public ItemStack getStackInSlot(int slot) {
		return this.crusherItemStacks[slot];
	}

	public ItemStack decrStackSize(int slot, int amount) {
		if (this.crusherItemStacks[slot] != null) {
			ItemStack stack;
			if (this.crusherItemStacks[slot].stackSize <= amount) {
				stack = this.crusherItemStacks[slot];
				this.crusherItemStacks[slot] = null;
				return stack;
			} else {
				stack = this.crusherItemStacks[slot].splitStack(amount);
				if (this.crusherItemStacks[slot].stackSize == 0) {
					this.crusherItemStacks[slot] = null;
				}
				return stack;
			}
		}
		return null;
	}

	public void setInventorySlotContents(int slot, ItemStack stack) {
		this.crusherItemStacks[slot] = stack;
		if (stack != null && stack.stackSize > this.getInventoryStackLimit()) {
			stack.stackSize = this.getInventoryStackLimit();
		}
	}

	public String getInvName() {
		return "Crusher";
	}

	public int getInventoryStackLimit() {
		return 64;
	}

	public int getCookProgressScaled(int scale) {
		return this.crusherCookTime * scale / 200;
	}

	public int getEnergyScaled(int scale) {
		return this.storedEnergy * scale / MAX_ENERGY;
	}

	public void updateEntity() {
		if (this.worldObj.multiplayerWorld) return;

		boolean changed = false;

		if (this.storedEnergy >= ENERGY_PER_TICK && this.canCrush()) {
			this.storedEnergy -= ENERGY_PER_TICK;
			++this.crusherCookTime;
			changed = true;
			if (this.crusherCookTime >= 200) {
				this.crusherCookTime = 0;
				this.crushItem();
			}
		} else if (!this.canCrush()) {
			this.crusherCookTime = 0;
		}

		if (changed) {
			this.onInventoryChanged();
		}
	}

	private boolean canCrush() {
		if (this.crusherItemStacks[0] == null) return false;
		ItemStack result = Retronism_RecipesCrusher.crushing().getCrushingResult(this.crusherItemStacks[0].getItem().shiftedIndex);
		if (result == null) return false;
		if (this.crusherItemStacks[1] == null) return true;
		if (!this.crusherItemStacks[1].isItemEqual(result)) return false;
		int combined = this.crusherItemStacks[1].stackSize + result.stackSize;
		return combined <= this.getInventoryStackLimit() && combined <= this.crusherItemStacks[1].getMaxStackSize();
	}

	public void crushItem() {
		if (this.canCrush()) {
			ItemStack result = Retronism_RecipesCrusher.crushing().getCrushingResult(this.crusherItemStacks[0].getItem().shiftedIndex);
			if (this.crusherItemStacks[1] == null) {
				this.crusherItemStacks[1] = result.copy();
			} else if (this.crusherItemStacks[1].itemID == result.itemID) {
				this.crusherItemStacks[1].stackSize += result.stackSize;
			}

			if (this.crusherItemStacks[0].getItem().hasContainerItem()) {
				this.crusherItemStacks[0] = new ItemStack(this.crusherItemStacks[0].getItem().getContainerItem());
			} else {
				--this.crusherItemStacks[0].stackSize;
			}

			if (this.crusherItemStacks[0].stackSize <= 0) {
				this.crusherItemStacks[0] = null;
			}
		}
	}

	public boolean canInteractWith(EntityPlayer player) {
		return this.worldObj.getBlockTileEntity(this.xCoord, this.yCoord, this.zCoord) == this
			&& player.getDistanceSq((double) this.xCoord + 0.5D, (double) this.yCoord + 0.5D, (double) this.zCoord + 0.5D) <= 64.0D;
	}

	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		NBTTagList list = nbt.getTagList("Items");
		this.crusherItemStacks = new ItemStack[this.getSizeInventory()];
		for (int i = 0; i < list.tagCount(); ++i) {
			NBTTagCompound tag = (NBTTagCompound) list.tagAt(i);
			byte slot = tag.getByte("Slot");
			if (slot >= 0 && slot < this.crusherItemStacks.length) {
				this.crusherItemStacks[slot] = new ItemStack(tag);
			}
		}
		this.crusherCookTime = nbt.getShort("CookTime");
		this.storedEnergy = nbt.getInteger("Energy");
		if (nbt.hasKey("SC0")) {
			for (int i = 0; i < 24; i++) this.sideConfig[i] = nbt.getInteger("SC" + i);
		}
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setShort("CookTime", (short) this.crusherCookTime);
		nbt.setInteger("Energy", this.storedEnergy);
		for (int i = 0; i < 24; i++) nbt.setInteger("SC" + i, this.sideConfig[i]);
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < this.crusherItemStacks.length; ++i) {
			if (this.crusherItemStacks[i] != null) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setByte("Slot", (byte) i);
				this.crusherItemStacks[i].writeToNBT(tag);
				list.setTag(tag);
			}
		}
		nbt.setTag("Items", list);
	}
}
