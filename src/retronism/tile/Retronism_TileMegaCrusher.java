package retronism.tile;

import net.minecraft.src.*;
import retronism.*;
import aero.machineapi.*;
import retronism.recipe.*;
import aero.modellib.Aero_AnimationBundle;
import aero.modellib.Aero_AnimationDefinition;
import aero.modellib.Aero_AnimationLoader;
import aero.modellib.Aero_AnimationState;

import java.util.Random;

public class Retronism_TileMegaCrusher extends TileEntity implements IInventory, Aero_IEnergyReceiver, Aero_ISideConfigurable, Aero_ISlotAccess {
	private ItemStack[] inventory = new ItemStack[6]; // 0=in1, 1=out1, 2=in2, 3=out2, 4=in3, 5=out3
	public int[] cookTime = new int[3];
	public int storedEnergy = 0;
	public static final int MAX_ENERGY = 64000;
	private static final int ENERGY_PER_TICK = 8;
	private static final int COOK_TIME = 200;
	private int[] sideConfig = new int[24];
	public int originX, originY, originZ;
	private int validationTimer = 0;
	private boolean isInvalidating = false;
	private Random rand = new Random();

	// --- Animation ---
	public static final int STATE_OFF = 0;
	public static final int STATE_ON  = 1;

	public static final Aero_AnimationBundle   BUNDLE   = Aero_AnimationLoader.load("/models/MegaCrusher.anim.json");
	public static final Aero_AnimationDefinition ANIM_DEF = new Aero_AnimationDefinition()
		.state(STATE_OFF, "idle")
		.state(STATE_ON,  "working");

	public final Aero_AnimationState animState = ANIM_DEF.createState(BUNDLE);

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

	public int[] getInsertSlots() { return new int[]{0, 2, 4}; }
	public int[] getExtractSlots() { return new int[]{1, 3, 5}; }

	public int receiveEnergy(int amount) {
		int space = MAX_ENERGY - storedEnergy;
		int accepted = Math.min(amount, space);
		storedEnergy += accepted;
		return accepted;
	}

	public int getStoredEnergy() { return storedEnergy; }
	public int getMaxEnergy() { return MAX_ENERGY; }

	public int getSizeInventory() { return inventory.length; }

	public ItemStack getStackInSlot(int slot) { return inventory[slot]; }

	public ItemStack decrStackSize(int slot, int amount) {
		if (inventory[slot] != null) {
			ItemStack stack;
			if (inventory[slot].stackSize <= amount) {
				stack = inventory[slot];
				inventory[slot] = null;
				return stack;
			} else {
				stack = inventory[slot].splitStack(amount);
				if (inventory[slot].stackSize == 0) inventory[slot] = null;
				return stack;
			}
		}
		return null;
	}

	public void setInventorySlotContents(int slot, ItemStack stack) {
		inventory[slot] = stack;
		if (stack != null && stack.stackSize > getInventoryStackLimit()) {
			stack.stackSize = getInventoryStackLimit();
		}
	}

	public String getInvName() { return "Mega Crusher"; }
	public int getInventoryStackLimit() { return 64; }

	public int getCookProgressScaled(int slot, int scale) {
		return cookTime[slot] * scale / COOK_TIME;
	}

	public int getEnergyScaled(int scale) {
		return storedEnergy * scale / MAX_ENERGY;
	}

	public void updateEntity() {
		animState.tick();
		boolean running = storedEnergy >= ENERGY_PER_TICK
			&& (cookTime[0] > 0 || cookTime[1] > 0 || cookTime[2] > 0);
		animState.setState(running ? STATE_ON : STATE_OFF);

		if (worldObj.multiplayerWorld) return;

		validationTimer++;
		if (validationTimer >= 20) {
			validationTimer = 0;
			if (!validateStructure()) {
				invalidateStructure();
				return;
			}
		}

		boolean changed = false;

		for (int i = 0; i < 3; i++) {
			int inputSlot = i * 2;
			int outputSlot = i * 2 + 1;

			if (storedEnergy >= ENERGY_PER_TICK && canCrush(inputSlot, outputSlot)) {
				storedEnergy -= ENERGY_PER_TICK;
				cookTime[i]++;
				changed = true;
				if (cookTime[i] >= COOK_TIME) {
					cookTime[i] = 0;
					crushItem(inputSlot, outputSlot);
				}
			} else if (!canCrush(inputSlot, outputSlot)) {
				cookTime[i] = 0;
			}
		}

		if (changed) onInventoryChanged();
	}

	private boolean canCrush(int inputSlot, int outputSlot) {
		if (inventory[inputSlot] == null) return false;
		ItemStack result = Retronism_RecipesCrusher.crushing().getCrushingResult(inventory[inputSlot].getItem().shiftedIndex);
		if (result == null) return false;
		if (inventory[outputSlot] == null) return true;
		if (!inventory[outputSlot].isItemEqual(result)) return false;
		int combined = inventory[outputSlot].stackSize + result.stackSize;
		return combined <= getInventoryStackLimit() && combined <= inventory[outputSlot].getMaxStackSize();
	}

	private void crushItem(int inputSlot, int outputSlot) {
		if (!canCrush(inputSlot, outputSlot)) return;
		ItemStack result = Retronism_RecipesCrusher.crushing().getCrushingResult(inventory[inputSlot].getItem().shiftedIndex);
		if (inventory[outputSlot] == null) {
			inventory[outputSlot] = result.copy();
		} else if (inventory[outputSlot].itemID == result.itemID) {
			inventory[outputSlot].stackSize += result.stackSize;
		}

		if (inventory[inputSlot].getItem().hasContainerItem()) {
			inventory[inputSlot] = new ItemStack(inventory[inputSlot].getItem().getContainerItem());
		} else {
			--inventory[inputSlot].stackSize;
		}
		if (inventory[inputSlot].stackSize <= 0) inventory[inputSlot] = null;
	}

	public boolean validateStructure() {
		for (int dx = 0; dx < 3; dx++) {
			for (int dy = 0; dy < 3; dy++) {
				for (int dz = 0; dz < 3; dz++) {
					int bx = originX + dx, by = originY + dy, bz = dz + originZ;
					int bid = worldObj.getBlockId(bx, by, bz);
					
					if (dx == 1 && dy == 1 && dz == 1) {
						if (bid != 0) return false;
					} else if (bx == xCoord && by == yCoord && bz == zCoord) {
						continue;
					} else {
						// Aceita tanto bloco normal quanto Port!
						if (bid == Retronism_Registry.testBlock.blockID) {
							continue;
						} else if (bid == Retronism_Registry.megaCrusherPortBlock.blockID) {
							// Linka o porto ao core
							TileEntity portTe = worldObj.getBlockTileEntity(bx, by, bz);
							if (portTe instanceof Aero_IPort) {
								((Aero_IPort) portTe).setCore(this);
							}
							continue;
						}
						return false;
					}
				}
			}
		}
		return true;
	}

	public void invalidateStructure() {
		if (isInvalidating) return;
		isInvalidating = true;

		for (int i = 0; i < inventory.length; i++) {
			if (inventory[i] != null) {
				float rx = rand.nextFloat() * 0.8F + 0.1F;
				float ry = rand.nextFloat() * 0.8F + 0.1F;
				float rz = rand.nextFloat() * 0.8F + 0.1F;
				ItemStack stack = inventory[i];
				while (stack.stackSize > 0) {
					int dropCount = rand.nextInt(21) + 10;
					if (dropCount > stack.stackSize) dropCount = stack.stackSize;
					stack.stackSize -= dropCount;
					EntityItem entity = new EntityItem(worldObj,
						(double)((float)xCoord + rx), (double)((float)yCoord + ry), (double)((float)zCoord + rz),
						new ItemStack(stack.itemID, dropCount, stack.getItemDamage()));
					float spread = 0.05F;
					entity.motionX = (double)((float)rand.nextGaussian() * spread);
					entity.motionY = (double)((float)rand.nextGaussian() * spread + 0.2F);
					entity.motionZ = (double)((float)rand.nextGaussian() * spread);
					worldObj.entityJoinedWorld(entity);
				}
				inventory[i] = null;
			}
		}

		worldObj.setBlockWithNotify(xCoord, yCoord, zCoord, Retronism_Registry.testBlock.blockID);
		isInvalidating = false;
	}

	public boolean canInteractWith(EntityPlayer player) {
		return worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this
			&& player.getDistanceSq((double)xCoord + 0.5D, (double)yCoord + 0.5D, (double)zCoord + 0.5D) <= 64.0D;
	}

	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		NBTTagList list = nbt.getTagList("Items");
		inventory = new ItemStack[getSizeInventory()];
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound tag = (NBTTagCompound) list.tagAt(i);
			byte slot = tag.getByte("Slot");
			if (slot >= 0 && slot < inventory.length) {
				inventory[slot] = new ItemStack(tag);
			}
		}
		for (int i = 0; i < 3; i++) cookTime[i] = nbt.getShort("CookTime" + i);
		storedEnergy = nbt.getInteger("Energy");
		originX = nbt.getInteger("OriginX");
		originY = nbt.getInteger("OriginY");
		originZ = nbt.getInteger("OriginZ");
		if (nbt.hasKey("SC0")) {
			for (int i = 0; i < 24; i++) sideConfig[i] = nbt.getInteger("SC" + i);
		}
		animState.readFromNBT(nbt);
	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		for (int i = 0; i < 3; i++) nbt.setShort("CookTime" + i, (short) cookTime[i]);
		nbt.setInteger("Energy", storedEnergy);
		nbt.setInteger("OriginX", originX);
		nbt.setInteger("OriginY", originY);
		nbt.setInteger("OriginZ", originZ);
		for (int i = 0; i < 24; i++) nbt.setInteger("SC" + i, sideConfig[i]);
		NBTTagList list = new NBTTagList();
		for (int i = 0; i < inventory.length; i++) {
			if (inventory[i] != null) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setByte("Slot", (byte) i);
				inventory[i].writeToNBT(tag);
				list.setTag(tag);
			}
		}
		nbt.setTag("Items", list);
		animState.writeToNBT(nbt);
	}
}
