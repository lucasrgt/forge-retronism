package retronism.tile;

import net.minecraft.src.*;
import retronism.api.*;

public class Retronism_TileItemPipe extends TileEntity implements IInventory, Retronism_ISideConfigurable {
	private ItemStack buffer = null;
	private static final int TRANSFER_COOLDOWN = 8;
	private int cooldown = 0;
	private int sourceX, sourceY, sourceZ;
	private boolean hasSource = false;
	private int[] sideConfig = new int[24];

	private static final int[][] DIRS = {
		{0,-1,0}, {0,1,0}, {0,0,-1}, {0,0,1}, {-1,0,0}, {1,0,0}
	};

	{
		for (int s = 0; s < 6; s++) {
			Retronism_SideConfig.set(sideConfig, s, Retronism_SideConfig.TYPE_ITEM, Retronism_SideConfig.MODE_INPUT_OUTPUT);
		}
	}

	public int[] getSideConfig() { return sideConfig; }
	public void setSideMode(int side, int type, int mode) {
		if (!supportsType(type)) return;
		int[] allowed = getAllowedModes(type);
		for (int m : allowed) { if (m == mode) { Retronism_SideConfig.set(sideConfig, side, type, mode); return; } }
	}
	public boolean supportsType(int type) {
		return type == Retronism_SideConfig.TYPE_ITEM;
	}
	public int[] getAllowedModes(int type) {
		if (type == Retronism_SideConfig.TYPE_ITEM) return new int[]{Retronism_SideConfig.MODE_NONE, Retronism_SideConfig.MODE_INPUT, Retronism_SideConfig.MODE_OUTPUT, Retronism_SideConfig.MODE_INPUT_OUTPUT};
		return new int[]{Retronism_SideConfig.MODE_NONE};
	}

	public int getSideMode(int side) {
		return Retronism_SideConfig.get(sideConfig, side, Retronism_SideConfig.TYPE_ITEM);
	}

	private boolean canSendTo(int side, TileEntity te) {
		if (!Retronism_SideConfig.canOutput(getSideMode(side))) return false;
		int oppSide = Retronism_SideConfig.oppositeSide(side);
		if (te instanceof Retronism_ISideConfigurable) {
			int neighborMode = Retronism_SideConfig.get(((Retronism_ISideConfigurable) te).getSideConfig(), oppSide, Retronism_SideConfig.TYPE_ITEM);
			if (!Retronism_SideConfig.canInput(neighborMode)) return false;
		}
		return true;
	}

	private boolean canReceiveFrom(int side) {
		return Retronism_SideConfig.canInput(getSideMode(side));
	}

	private int[] getExtractSlotsFor(TileEntity te) {
		if (te instanceof Retronism_ISlotAccess) return ((Retronism_ISlotAccess) te).getExtractSlots();
		if (te instanceof TileEntityFurnace) return new int[]{2};
		return null;
	}

	private int[] getInsertSlotsFor(TileEntity te) {
		if (te instanceof Retronism_ISlotAccess) return ((Retronism_ISlotAccess) te).getInsertSlots();
		if (te instanceof TileEntityFurnace) return new int[]{0, 1};
		return null;
	}

	public void updateEntity() {
		if (this.worldObj.multiplayerWorld) return;

		// Pull items from neighboring inventories on input sides
		if (buffer == null) {
			for (int side = 0; side < 6; side++) {
				if (!canReceiveFrom(side)) continue;
				int[] d = DIRS[side];
				TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
				if (te == null || te instanceof Retronism_TileItemPipe) continue;
				if (!(te instanceof IInventory)) continue;
				int oppSide = Retronism_SideConfig.oppositeSide(side);
				if (te instanceof Retronism_ISideConfigurable) {
					int neighborMode = Retronism_SideConfig.get(((Retronism_ISideConfigurable) te).getSideConfig(), oppSide, Retronism_SideConfig.TYPE_ITEM);
					if (!Retronism_SideConfig.canOutput(neighborMode)) continue;
				}
				IInventory inv = (IInventory) te;
				int[] extractSlots = getExtractSlotsFor(te);
				if (extractSlots != null) {
					for (int slot : extractSlots) {
						ItemStack stack = inv.getStackInSlot(slot);
						if (stack != null) {
							buffer = inv.decrStackSize(slot, 1);
							cooldown = TRANSFER_COOLDOWN;
							sourceX = xCoord + d[0]; sourceY = yCoord + d[1]; sourceZ = zCoord + d[2];
							hasSource = true;
							return;
						}
					}
				} else {
					for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
						ItemStack stack = inv.getStackInSlot(slot);
						if (stack != null) {
							buffer = inv.decrStackSize(slot, 1);
							cooldown = TRANSFER_COOLDOWN;
							sourceX = xCoord + d[0]; sourceY = yCoord + d[1]; sourceZ = zCoord + d[2];
							hasSource = true;
							return;
						}
					}
				}
			}
			return;
		}

		// Cooldown before pushing
		if (cooldown > 0) { cooldown--; return; }

		// Push buffer to neighboring inventories/pipes on output sides
		for (int side = 0; side < 6; side++) {
			if (buffer == null) break;
			int[] d = DIRS[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te == null) continue;
			if (!canSendTo(side, te)) continue;

			// Skip the machine we extracted from
			if (hasSource && xCoord + d[0] == sourceX && yCoord + d[1] == sourceY && zCoord + d[2] == sourceZ) continue;

			if (te instanceof Retronism_TileItemPipe) {
				Retronism_TileItemPipe other = (Retronism_TileItemPipe) te;
				if (other.buffer == null) {
					other.buffer = buffer;
					other.cooldown = TRANSFER_COOLDOWN;
					buffer = null;
					hasSource = false;
				}
			} else if (te instanceof IInventory) {
				IInventory inv = (IInventory) te;
				int[] insertSlots = getInsertSlotsFor(te);
				buffer = addToInventory(inv, buffer, insertSlots);
				if (buffer == null) hasSource = false;
			}
		}
	}

	private ItemStack addToInventory(IInventory inv, ItemStack stack, int[] slots) {
		if (slots != null) {
			// Try to merge with existing stacks in allowed slots
			for (int i : slots) {
				ItemStack existing = inv.getStackInSlot(i);
				if (existing != null && existing.itemID == stack.itemID && existing.getItemDamage() == stack.getItemDamage()) {
					int space = existing.getMaxStackSize() - existing.stackSize;
					if (space > 0) {
						int toAdd = Math.min(stack.stackSize, space);
						existing.stackSize += toAdd;
						stack.stackSize -= toAdd;
						if (stack.stackSize <= 0) return null;
					}
				}
			}
			// Try empty allowed slots
			for (int i : slots) {
				if (inv.getStackInSlot(i) == null) {
					inv.setInventorySlotContents(i, stack);
					return null;
				}
			}
		} else {
			// Fallback: all slots (chests, etc.)
			for (int i = 0; i < inv.getSizeInventory(); i++) {
				ItemStack existing = inv.getStackInSlot(i);
				if (existing != null && existing.itemID == stack.itemID && existing.getItemDamage() == stack.getItemDamage()) {
					int space = existing.getMaxStackSize() - existing.stackSize;
					if (space > 0) {
						int toAdd = Math.min(stack.stackSize, space);
						existing.stackSize += toAdd;
						stack.stackSize -= toAdd;
						if (stack.stackSize <= 0) return null;
					}
				}
			}
			for (int i = 0; i < inv.getSizeInventory(); i++) {
				if (inv.getStackInSlot(i) == null) {
					inv.setInventorySlotContents(i, stack);
					return null;
				}
			}
		}
		return stack;
	}

	// IInventory - single slot buffer
	public int getSizeInventory() { return 1; }
	public ItemStack getStackInSlot(int slot) { return slot == 0 ? buffer : null; }
	public ItemStack decrStackSize(int slot, int amount) {
		if (slot != 0 || buffer == null) return null;
		if (amount >= buffer.stackSize) {
			ItemStack result = buffer;
			buffer = null;
			return result;
		}
		buffer.stackSize -= amount;
		return new ItemStack(buffer.itemID, amount, buffer.getItemDamage());
	}
	public ItemStack getStackInSlotOnClosing(int slot) { return null; }
	public void setInventorySlotContents(int slot, ItemStack stack) {
		if (slot == 0) buffer = stack;
	}
	public String getInvName() { return "Item Pipe"; }
	public int getInventoryStackLimit() { return 64; }
	public boolean isUseableByPlayer(EntityPlayer player) { return false; }
	public boolean canInteractWith(EntityPlayer player) { return false; }
	public void openChest() {}
	public void closeChest() {}

	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		if (nbt.hasKey("Buffer")) {
			NBTTagCompound bufTag = nbt.getCompoundTag("Buffer");
			buffer = new ItemStack(bufTag.getShort("id"), bufTag.getByte("Count"), bufTag.getShort("Damage"));
		}
		cooldown = nbt.getInteger("Cooldown");
		if (nbt.hasKey("SC0")) {
			for (int i = 0; i < 24; i++) this.sideConfig[i] = nbt.getInteger("SC" + i);
		}
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		if (buffer != null) {
			NBTTagCompound bufTag = new NBTTagCompound();
			buffer.writeToNBT(bufTag);
			nbt.setCompoundTag("Buffer", bufTag);
		}
		nbt.setInteger("Cooldown", cooldown);
		for (int i = 0; i < 24; i++) nbt.setInteger("SC" + i, this.sideConfig[i]);
	}
}
