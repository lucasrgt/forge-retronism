package retronism.tile;

import net.minecraft.src.*;
import retronism.api.Retronism_IEnergyReceiver;
import retronism.Retronism_Registry;
public class Retronism_TileAlloyBlender extends TileEntity implements IInventory, Retronism_IEnergyReceiver {

    private ItemStack[] inventory = new ItemStack[4];
    public boolean isFormed = false;

    private int storedEnergy = 0;
    private int maxEnergy = 16000;

    public int processTime = 0;
    public int maxProcessTime = 200;
    private int energyPerTick = 8;

    private static final int TYPE_AIR = 0;
    private static final int TYPE_CONTROLLER = 1;
    private static final int TYPE_IRON_BLOCK = 2;
    private static final int TYPE_COUNT = 3;

    private static final int[][][] STRUCTURE = {
        { // Layer 0
            {2, 2, 2},
            {2, 2, 2},
            {2, 2, 2},
        },
        { // Layer 1
            {2, 2, 2},
            {2, 0, 2},
            {2, 2, 2},
        },
        { // Layer 2
            {2, 2, 2},
            {2, 2, 2},
            {2, 2, 2},
        },
    };


    private String lastFailDebug = null;
    public String getLastFailDebug() { return lastFailDebug; }

    public boolean checkStructure(World world, int cx, int cy, int cz) {
        int ctrlX = -1, ctrlY = -1, ctrlZ = -1;
        for (int y = 0; y < STRUCTURE.length; y++)
            for (int z = 0; z < STRUCTURE[y].length; z++)
                for (int x = 0; x < STRUCTURE[y][z].length; x++)
                    if (STRUCTURE[y][z][x] == TYPE_CONTROLLER) { ctrlX = x; ctrlY = y; ctrlZ = z; }
        if (ctrlX == -1) { isFormed = false; return false; }

        int[] expectedIds = new int[TYPE_COUNT];
        expectedIds[TYPE_CONTROLLER] = Retronism_Registry.alloyBlenderControllerBlock.blockID;
        expectedIds[TYPE_IRON_BLOCK] = 42;

        int[][] facings = {
            { 1, 0, 0, 1},
            { 0, 1,-1, 0},
            {-1, 0, 0,-1},
            { 0,-1, 1, 0},
        };

        lastFailDebug = null;
        String allFails = "";

        for (int f = 0; f < 4; f++) {
            boolean ok = true;
            for (int sy = 0; sy < STRUCTURE.length && ok; sy++) {
                for (int sz = 0; sz < STRUCTURE[sy].length && ok; sz++) {
                    for (int sx = 0; sx < STRUCTURE[sy][sz].length && ok; sx++) {
                        int expected = STRUCTURE[sy][sz][sx];
                        if (expected == TYPE_AIR) continue;
                        int relX = sx - ctrlX;
                        int relZ = sz - ctrlZ;
                        int wx = cx + relX * facings[f][0] + relZ * facings[f][2];
                        int wy = cy + (sy - ctrlY);
                        int wz = cz + relX * facings[f][1] + relZ * facings[f][3];
                        int blockId = world.getBlockId(wx, wy, wz);
                        boolean match = (blockId == expectedIds[expected]);
                        if (!match) {
                            allFails += "rot" + f + ":s(" + sx + "," + sy + "," + sz + ")w(" + wx + "," + wy + "," + wz + ")exp=" + expectedIds[expected] + "got=" + blockId + "" | ";
                            ok = false;
                        }
                    }
                }
            }
            if (ok) {
                boolean wasFormed = isFormed;
                isFormed = true;
                return true;
            }
        }

        lastFailDebug = allFails;
        isFormed = false;
        return false;
    }

    @Override
    public void updateEntity() {
        if (worldObj.multiplayerWorld) return;

        if (!isFormed) return;

        boolean canProcess = canProcess();
        if (canProcess && storedEnergy >= energyPerTick) {
            storedEnergy -= energyPerTick;
            processTime++;
            if (processTime >= maxProcessTime) {
                processTime = 0;
                processItem();
            }
        } else if (!canProcess) {
            processTime = 0;
        }
    }

    private boolean canProcess() {
        if (inventory[0] == null) return false;
        // TODO: Add recipe lookup here
        return true;
    }

    private void processItem() {
        // TODO: Add recipe processing here
    }

    // --- IInventory ---
    @Override
    public int getSizeInventory() { return inventory.length; }

    @Override
    public ItemStack getStackInSlot(int slot) { return inventory[slot]; }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (inventory[slot] != null) {
            if (inventory[slot].stackSize <= amount) {
                ItemStack stack = inventory[slot];
                inventory[slot] = null;
                return stack;
            }
            ItemStack split = inventory[slot].splitStack(amount);
            if (inventory[slot].stackSize == 0) inventory[slot] = null;
            return split;
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        inventory[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
    }

    @Override
    public String getInvName() { return "AlloyBlender"; }

    @Override
    public int getInventoryStackLimit() { return 64; }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this
            && player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64.0;
    }

    // --- IEnergyReceiver ---
    @Override
    public int receiveEnergy(int amount) {
        int accepted = Math.min(amount, maxEnergy - storedEnergy);
        storedEnergy += accepted;
        return accepted;
    }

    @Override
    public int getStoredEnergy() { return storedEnergy; }

    @Override
    public int getMaxEnergy() { return maxEnergy; }

    public int getEnergyScaled(int scale) {
        return maxEnergy > 0 ? storedEnergy * scale / maxEnergy : 0;
    }

    public int getCookProgressScaled(int scale) {
        return maxProcessTime > 0 ? processTime * scale / maxProcessTime : 0;
    }

    // --- NBT ---
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        isFormed = nbt.getBoolean("Formed");
        storedEnergy = nbt.getInteger("Energy");
        processTime = nbt.getShort("ProcessTime");

        NBTTagList items = nbt.getTagList("Items");
        inventory = new ItemStack[4];
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound slot = (NBTTagCompound) items.tagAt(i);
            int idx = slot.getByte("Slot") & 255;
            if (idx < inventory.length) {
                int id = slot.getShort("id");
                int count = slot.getByte("Count");
                int dmg = slot.getShort("Damage");
                inventory[idx] = new ItemStack(id, count, dmg);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("Formed", isFormed);
        nbt.setInteger("Energy", storedEnergy);
        nbt.setShort("ProcessTime", (short) processTime);

        NBTTagList items = new NBTTagList();
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] != null) {
                NBTTagCompound slot = new NBTTagCompound();
                slot.setByte("Slot", (byte) i);
                slot.setShort("id", (short) inventory[i].itemID);
                slot.setByte("Count", (byte) inventory[i].stackSize);
                slot.setShort("Damage", (short) inventory[i].getItemDamage());
                items.setTag(slot);
            }
        }
        nbt.setTag("Items", items);
    }
}
