package retronism.tile;

import net.minecraft.src.*;
import aero.machineapi.*;

public class Retronism_TileGenerator extends TileEntity implements IInventory, Aero_ISideConfigurable, Aero_ISlotAccess {
	private ItemStack[] generatorItems = new ItemStack[1]; // fuel slot only
	public int burnTime = 0;
	public int currentItemBurnTime = 0;
	public int storedEnergy = 0;
	public static final int MAX_ENERGY = 32000;
	private static final int ENERGY_PER_TICK = 32;
	private static final int PUSH_RATE = 200;
	public int lastOutput = 0;
	private int[] sideConfig = new int[24];

	{
		for (int s = 0; s < 6; s++) {
			Aero_SideConfig.set(sideConfig, s, Aero_SideConfig.TYPE_ENERGY, Aero_SideConfig.MODE_OUTPUT);
			Aero_SideConfig.set(sideConfig, s, Aero_SideConfig.TYPE_ITEM, Aero_SideConfig.MODE_INPUT);
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
		if (type == Aero_SideConfig.TYPE_ENERGY) return new int[]{Aero_SideConfig.MODE_NONE, Aero_SideConfig.MODE_OUTPUT};
		if (type == Aero_SideConfig.TYPE_ITEM) return new int[]{Aero_SideConfig.MODE_NONE, Aero_SideConfig.MODE_INPUT};
		return new int[]{Aero_SideConfig.MODE_NONE};
	}

	public int[] getInsertSlots() { return new int[]{0}; }
	public int[] getExtractSlots() { return new int[]{}; }

	public int getSizeInventory() {
		return this.generatorItems.length;
	}

	public ItemStack getStackInSlot(int slot) {
		return this.generatorItems[slot];
	}

	public ItemStack decrStackSize(int slot, int amount) {
		if (this.generatorItems[slot] != null) {
			ItemStack stack;
			if (this.generatorItems[slot].stackSize <= amount) {
				stack = this.generatorItems[slot];
				this.generatorItems[slot] = null;
				return stack;
			} else {
				stack = this.generatorItems[slot].splitStack(amount);
				if (this.generatorItems[slot].stackSize == 0) {
					this.generatorItems[slot] = null;
				}
				return stack;
			}
		}
		return null;
	}

	public void setInventorySlotContents(int slot, ItemStack stack) {
		this.generatorItems[slot] = stack;
		if (stack != null && stack.stackSize > this.getInventoryStackLimit()) {
			stack.stackSize = this.getInventoryStackLimit();
		}
	}

	public String getInvName() {
		return "Generator";
	}

	public int getInventoryStackLimit() {
		return 64;
	}

	public boolean isBurning() {
		return this.burnTime > 0;
	}

	public int getEnergyScaled(int scale) {
		return this.storedEnergy * scale / MAX_ENERGY;
	}

	public int getBurnTimeRemainingScaled(int scale) {
		if (this.currentItemBurnTime == 0) {
			this.currentItemBurnTime = 200;
		}
		return this.burnTime * scale / this.currentItemBurnTime;
	}

	public void updateEntity() {
		if (this.worldObj.multiplayerWorld) return;

		boolean changed = false;

		if (this.burnTime > 0) {
			--this.burnTime;
			if (storedEnergy < MAX_ENERGY) {
				storedEnergy = Math.min(storedEnergy + ENERGY_PER_TICK, MAX_ENERGY);
				changed = true;
			}
		}

		// Try to consume fuel if not burning and energy not full
		if (this.burnTime == 0 && storedEnergy < MAX_ENERGY) {
			int fuelTime = getItemBurnTime(this.generatorItems[0]);
			if (fuelTime > 0) {
				this.currentItemBurnTime = this.burnTime = fuelTime;
				changed = true;
				if (this.generatorItems[0] != null) {
					if (this.generatorItems[0].getItem().hasContainerItem()) {
						this.generatorItems[0] = new ItemStack(this.generatorItems[0].getItem().getContainerItem());
					} else {
						--this.generatorItems[0].stackSize;
					}
					if (this.generatorItems[0].stackSize == 0) {
						this.generatorItems[0] = null;
					}
				}
			}
		}

		// Push energy to adjacent blocks
		int totalPushed = 0;
		if (storedEnergy > 0) {
			int[][] dirs = {{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
			for (int side = 0; side < 6; side++) {
				if (storedEnergy <= 0) break;
				int myMode = Aero_SideConfig.get(sideConfig, side, Aero_SideConfig.TYPE_ENERGY);
				if (!Aero_SideConfig.canOutput(myMode)) continue;
				int[] d = dirs[side];
				TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
				if (te instanceof Aero_IEnergyReceiver) {
					int oppSide = Aero_SideConfig.oppositeSide(side);
					if (te instanceof Aero_ISideConfigurable) {
						int neighborMode = Aero_SideConfig.get(((Aero_ISideConfigurable) te).getSideConfig(), oppSide, Aero_SideConfig.TYPE_ENERGY);
						if (!Aero_SideConfig.canInput(neighborMode)) continue;
					}
					Aero_IEnergyReceiver recv = (Aero_IEnergyReceiver) te;
					int toSend = Math.min(PUSH_RATE, storedEnergy);
					int accepted = recv.receiveEnergy(toSend);
					if (accepted > 0) {
						storedEnergy -= accepted;
						totalPushed += accepted;
						changed = true;
					}
				}
			}
		}
		this.lastOutput = totalPushed;

		if (changed) {
			this.onInventoryChanged();
		}
	}

	private int getItemBurnTime(ItemStack stack) {
		if (stack == null) return 0;
		int id = stack.getItem().shiftedIndex;
		if (id < 256 && Block.blocksList[id].blockMaterial == Material.wood) return 300;
		if (id == Item.stick.shiftedIndex) return 100;
		if (id == Item.coal.shiftedIndex) return 1600;
		if (id == Item.bucketLava.shiftedIndex) return 20000;
		if (id == Block.sapling.blockID) return 100;
		return ModLoader.AddAllFuel(id);
	}

	public boolean canInteractWith(EntityPlayer player) {
		return this.worldObj.getBlockTileEntity(this.xCoord, this.yCoord, this.zCoord) == this
			&& player.getDistanceSq((double) this.xCoord + 0.5D, (double) this.yCoord + 0.5D, (double) this.zCoord + 0.5D) <= 64.0D;
	}

	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		NBTTagList list = nbt.getTagList("Items");
		this.generatorItems = new ItemStack[this.getSizeInventory()];
		for (int i = 0; i < list.tagCount(); ++i) {
			NBTTagCompound tag = (NBTTagCompound) list.tagAt(i);
			byte slot = tag.getByte("Slot");
			if (slot >= 0 && slot < this.generatorItems.length) {
				this.generatorItems[slot] = new ItemStack(tag);
			}
		}
		this.burnTime = nbt.getShort("BurnTime");
		this.currentItemBurnTime = nbt.getShort("CurrentBurn");
		this.storedEnergy = nbt.getInteger("Energy");
		if (nbt.hasKey("SC0")) {
			for (int i = 0; i < 24; i++) this.sideConfig[i] = nbt.getInteger("SC" + i);
		}
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setShort("BurnTime", (short) this.burnTime);
		nbt.setShort("CurrentBurn", (short) this.currentItemBurnTime);
		nbt.setInteger("Energy", this.storedEnergy);
		for (int i = 0; i < 24; i++) nbt.setInteger("SC" + i, this.sideConfig[i]);
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < this.generatorItems.length; ++i) {
			if (this.generatorItems[i] != null) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setByte("Slot", (byte) i);
				this.generatorItems[i].writeToNBT(tag);
				list.setTag(tag);
			}
		}
		nbt.setTag("Items", list);
	}
}
