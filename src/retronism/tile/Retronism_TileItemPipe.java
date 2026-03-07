package retronism.tile;

import net.minecraft.src.*;
import retronism.api.*;

public class Retronism_TileItemPipe extends TileEntity implements IInventory, Retronism_ISideConfigurable {
	private ItemStack buffer = null;
	private static final int TRANSFER_COOLDOWN = 8;
	private int cooldown = 0;
	private int lastReceivedFrom = -1;
	private int[] sideConfig = new int[24];

	// Priority: 0 = highest priority, 9 = lowest. Default 5.
	private int priority = 5;
	private int[] sidePriority = new int[]{5, 5, 5, 5, 5, 5};

	// Filter: 9 ghost slots, whitelist/blacklist
	private ItemStack[] filterSlots = new ItemStack[9];
	private boolean whitelist = false;

	private static final int[][] DIRS = {
		{0,-1,0}, {0,1,0}, {0,0,-1}, {0,0,1}, {-1,0,0}, {1,0,0}
	};

	{
		for (int s = 0; s < 6; s++) {
			Retronism_SideConfig.set(sideConfig, s, Retronism_SideConfig.TYPE_ITEM, Retronism_SideConfig.MODE_INPUT_OUTPUT);
		}
	}

	// --- Priority accessors ---
	public int getPriority() { return priority; }
	public void setPriority(int p) { priority = Math.max(0, Math.min(9, p)); }
	public int getSidePriority(int side) { return sidePriority[side]; }
	public void setSidePriority(int side, int p) { sidePriority[side] = Math.max(0, Math.min(9, p)); }

	// --- Filter accessors ---
	public ItemStack getFilterSlot(int i) { return i >= 0 && i < 9 ? filterSlots[i] : null; }
	public void setFilterSlot(int i, ItemStack stack) {
		if (i >= 0 && i < 9) filterSlots[i] = stack;
	}
	public boolean isWhitelist() { return whitelist; }
	public void setWhitelist(boolean w) { whitelist = w; }

	public boolean passesFilter(ItemStack stack) {
		if (stack == null) return false;
		boolean hasFilter = false;
		boolean matchesFilter = false;
		for (int i = 0; i < 9; i++) {
			if (filterSlots[i] != null) {
				hasFilter = true;
				if (filterSlots[i].itemID == stack.itemID && filterSlots[i].getItemDamage() == stack.getItemDamage()) {
					matchesFilter = true;
					break;
				}
			}
		}
		if (!hasFilter) return !whitelist;
		return whitelist ? matchesFilter : !matchesFilter;
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
		if (te instanceof TileEntityFurnace) return new int[]{0};
		return null;
	}

	private boolean hasViableOutput(int excludeX, int excludeY, int excludeZ) {
		for (int side = 0; side < 6; side++) {
			if (!Retronism_SideConfig.canOutput(getSideMode(side))) continue;
			int[] d = DIRS[side];
			int nx = xCoord + d[0], ny = yCoord + d[1], nz = zCoord + d[2];
			if (nx == excludeX && ny == excludeY && nz == excludeZ) continue;
			TileEntity te = worldObj.getBlockTileEntity(nx, ny, nz);
			if (te == null) continue;
			if (te instanceof Retronism_TileItemPipe) return true;
			if (te instanceof Retronism_TileMegaPipe) return true;
			if (te instanceof IInventory && canSendTo(side, te)) return true;
		}
		return false;
	}

	public boolean receiveItem(ItemStack stack, int fromSide) {
		if (buffer != null) return false;
		if (!passesFilter(stack)) return false;
		this.buffer = stack;
		this.cooldown = TRANSFER_COOLDOWN;
		this.lastReceivedFrom = fromSide;
		return true;
	}

	private int getOutputPriority(int side, TileEntity te) {
		if (te instanceof Retronism_TileItemPipe) {
			return ((Retronism_TileItemPipe) te).priority;
		}
		return sidePriority[side];
	}

	private int[] getSortedOutputSides() {
		int[] sides = new int[6];
		int[] priorities = new int[6];
		int count = 0;
		for (int side = 0; side < 6; side++) {
			if (side == lastReceivedFrom) continue;
			if (!Retronism_SideConfig.canOutput(getSideMode(side))) continue;
			int[] d = DIRS[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te == null) continue;
			if (!canSendTo(side, te)) continue;
			sides[count] = side;
			priorities[count] = getOutputPriority(side, te);
			count++;
		}
		// Simple insertion sort by priority (lower = higher priority)
		for (int i = 1; i < count; i++) {
			int key = priorities[i];
			int keySide = sides[i];
			int j = i - 1;
			while (j >= 0 && priorities[j] > key) {
				priorities[j + 1] = priorities[j];
				sides[j + 1] = sides[j];
				j--;
			}
			priorities[j + 1] = key;
			sides[j + 1] = keySide;
		}
		int[] result = new int[count];
		for (int i = 0; i < count; i++) result[i] = sides[i];
		return result;
	}

	public void updateEntity() {
		if (this.worldObj.multiplayerWorld) return;

		// Pull items from neighboring inventories on input sides
		if (buffer == null) {
			lastReceivedFrom = -1;
			for (int side = 0; side < 6; side++) {
				if (!canReceiveFrom(side)) continue;
				int[] d = DIRS[side];
				TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
				if (te == null || te instanceof Retronism_TileItemPipe || te instanceof Retronism_TileMegaPipe) continue;
				if (!(te instanceof IInventory)) continue;
				int oppSide = Retronism_SideConfig.oppositeSide(side);
				if (te instanceof Retronism_ISideConfigurable) {
					int neighborMode = Retronism_SideConfig.get(((Retronism_ISideConfigurable) te).getSideConfig(), oppSide, Retronism_SideConfig.TYPE_ITEM);
					if (!Retronism_SideConfig.canOutput(neighborMode)) continue;
				}
				// Only extract if there's somewhere else to send the item
				if (!hasViableOutput(xCoord + d[0], yCoord + d[1], zCoord + d[2])) continue;
				IInventory inv = (IInventory) te;
				int[] extractSlots = getExtractSlotsFor(te);
				if (extractSlots != null) {
					for (int slot : extractSlots) {
						ItemStack stack = inv.getStackInSlot(slot);
						if (stack != null && passesFilter(stack)) {
							buffer = inv.decrStackSize(slot, 1);
							cooldown = TRANSFER_COOLDOWN;
							lastReceivedFrom = side;
							return;
						}
					}
				} else {
					for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
						ItemStack stack = inv.getStackInSlot(slot);
						if (stack != null && passesFilter(stack)) {
							buffer = inv.decrStackSize(slot, 1);
							cooldown = TRANSFER_COOLDOWN;
							lastReceivedFrom = side;
							return;
						}
					}
				}
			}
			return;
		}

		// Cooldown before pushing
		if (cooldown > 0) { cooldown--; return; }

		// Push buffer to neighbors sorted by priority
		int[] sortedSides = getSortedOutputSides();
		for (int i = 0; i < sortedSides.length; i++) {
			if (buffer == null) break;
			int side = sortedSides[i];
			int[] d = DIRS[side];
			TileEntity te = worldObj.getBlockTileEntity(xCoord + d[0], yCoord + d[1], zCoord + d[2]);
			if (te == null) continue;

			if (te instanceof Retronism_TileItemPipe) {
				Retronism_TileItemPipe other = (Retronism_TileItemPipe) te;
				int oppSide = Retronism_SideConfig.oppositeSide(side);
				if (other.receiveItem(buffer, oppSide)) {
					buffer = null;
					lastReceivedFrom = -1;
				}
			} else if (te instanceof Retronism_TileMegaPipe) {
				Retronism_TileMegaPipe mega = (Retronism_TileMegaPipe) te;
				if (mega.itemBuffer == null) {
					int oppSide = Retronism_SideConfig.oppositeSide(side);
					mega.receiveItem(buffer, oppSide);
					buffer = null;
					lastReceivedFrom = -1;
				}
			} else if (te instanceof IInventory) {
				IInventory inv = (IInventory) te;
				int[] insertSlots = getInsertSlotsFor(te);
				buffer = addToInventory(inv, buffer, insertSlots);
				if (buffer == null) lastReceivedFrom = -1;
			}
		}
	}

	private ItemStack addToInventory(IInventory inv, ItemStack stack, int[] slots) {
		if (slots != null) {
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
			for (int i : slots) {
				if (inv.getStackInSlot(i) == null) {
					inv.setInventorySlotContents(i, stack);
					return null;
				}
			}
		} else {
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
		lastReceivedFrom = nbt.getInteger("LastFrom");
		if (lastReceivedFrom < -1 || lastReceivedFrom > 5) lastReceivedFrom = -1;
		priority = nbt.getInteger("Priority");
		if (priority < 0 || priority > 9) priority = 5;
		for (int i = 0; i < 6; i++) {
			sidePriority[i] = nbt.getInteger("SP" + i);
			if (sidePriority[i] < 0 || sidePriority[i] > 9) sidePriority[i] = 5;
		}
		whitelist = nbt.getBoolean("Whitelist");
		NBTTagList filterList = nbt.getTagList("Filters");
		for (int i = 0; i < filterList.tagCount(); i++) {
			NBTTagCompound tag = (NBTTagCompound) filterList.tagAt(i);
			int slot = tag.getByte("Slot");
			if (slot >= 0 && slot < 9) {
				filterSlots[slot] = new ItemStack(tag.getShort("id"), 1, tag.getShort("Damage"));
			}
		}
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
		nbt.setInteger("LastFrom", lastReceivedFrom);
		nbt.setInteger("Priority", priority);
		for (int i = 0; i < 6; i++) {
			nbt.setInteger("SP" + i, sidePriority[i]);
		}
		nbt.setBoolean("Whitelist", whitelist);
		NBTTagList filterList = new NBTTagList();
		for (int i = 0; i < 9; i++) {
			if (filterSlots[i] != null) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setByte("Slot", (byte) i);
				tag.setShort("id", (short) filterSlots[i].itemID);
				tag.setShort("Damage", (short) filterSlots[i].getItemDamage());
				filterList.setTag(tag);
			}
		}
		nbt.setTag("Filters", filterList);
		for (int i = 0; i < 24; i++) nbt.setInteger("SC" + i, this.sideConfig[i]);
	}
}
