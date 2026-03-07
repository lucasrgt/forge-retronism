package retronism.tile;

import net.minecraft.src.*;
import retronism.api.Retronism_IFluidHandler;
import retronism.api.Retronism_FluidType;
import retronism.api.Retronism_IGasHandler;
import retronism.api.Retronism_GasType;
import retronism.api.Retronism_IEnergyReceiver;

public class Retronism_TileMegaElectrolysis extends TileEntity implements IInventory, Retronism_IEnergyReceiver, Retronism_IFluidHandler, Retronism_IGasHandler {

    private ItemStack[] inventory = new ItemStack[0];
    public boolean isFormed = false;

    private int storedEnergy = 0;
    private int maxEnergy = 192000;

    private int fluidType = Retronism_FluidType.NONE;
    private int fluidAmount = 0;
    private int fluidCapacity = 3000;

    private int gasType = Retronism_GasType.NONE;
    private int gasAmount = 0;
    private int gasCapacity = 4500;

    public int processTime = 0;
    public int maxProcessTime = 200;
    private int energyPerTick = 48;

    private static final int TYPE_AIR = 0;
    private static final int TYPE_CASING = 1;
    private static final int TYPE_FLUID_PORT = 2;
    private static final int TYPE_GLASS = 3;
    private static final int TYPE_CONTROLLER = 4;
    private static final int TYPE_ENERGY_PORT = 5;
    private static final int TYPE_GAS_PORT = 6;

    private static final int[][][] STRUCTURE = {
        { // Layer 0
            {1, 2, 1},
            {2, 1, 2},
            {1, 2, 1},
        },
        { // Layer 1
            {3, 4, 3},
            {5, 0, 5},
            {3, 3, 3},
        },
        { // Layer 2
            {3, 3, 3},
            {5, 0, 5},
            {3, 3, 3},
        },
        { // Layer 3
            {1, 6, 1},
            {6, 1, 6},
            {1, 6, 1},
        },
    };


    public boolean checkStructure(World world, int cx, int cy, int cz) {
        int ctrlX = -1, ctrlY = -1, ctrlZ = -1;
        for (int y = 0; y < STRUCTURE.length; y++) {
            for (int z = 0; z < STRUCTURE[y].length; z++) {
                for (int x = 0; x < STRUCTURE[y][z].length; x++) {
                    if (STRUCTURE[y][z][x] == TYPE_CONTROLLER) {
                        ctrlX = x; ctrlY = y; ctrlZ = z;
                    }
                }
            }
        }
        if (ctrlX == -1) { isFormed = false; return false; }

        int casingId = mod_Retronism.blockMegaElectrolysisCasing.blockID;
        int controllerId = mod_Retronism.blockMegaElectrolysisController.blockID;

        for (int y = 0; y < STRUCTURE.length; y++) {
            for (int z = 0; z < STRUCTURE[y].length; z++) {
                for (int x = 0; x < STRUCTURE[y][z].length; x++) {
                    int expected = STRUCTURE[y][z][x];
                    if (expected == TYPE_AIR) continue;

                    int wx = cx + (x - ctrlX);
                    int wy = cy + (y - ctrlY);
                    int wz = cz + (z - ctrlZ);
                    int blockId = world.getBlockId(wx, wy, wz);

                    if (expected == TYPE_CONTROLLER) {
                        if (blockId != controllerId) { isFormed = false; return false; }
                    } else {
                        if (blockId != casingId && blockId != controllerId) {
                            isFormed = false;
                            return false;
                        }
                    }
                }
            }
        }
        isFormed = true;
        return true;
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
    public String getInvName() { return "MegaElectrolysis"; }

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

    // --- IFluidHandler ---
    @Override
    public int receiveFluid(int type, int amountMB) {
        if (fluidType != Retronism_FluidType.NONE && fluidType != type) return 0;
        int accepted = Math.min(amountMB, fluidCapacity - fluidAmount);
        if (accepted > 0) { fluidType = type; fluidAmount += accepted; }
        return accepted;
    }

    @Override
    public int extractFluid(int type, int amountMB) {
        if (fluidType != type) return 0;
        int extracted = Math.min(amountMB, fluidAmount);
        fluidAmount -= extracted;
        if (fluidAmount <= 0) { fluidAmount = 0; fluidType = Retronism_FluidType.NONE; }
        return extracted;
    }

    @Override public int getFluidType() { return fluidType; }
    @Override public int getFluidAmount() { return fluidAmount; }
    @Override public int getFluidCapacity() { return fluidCapacity; }

    // --- IGasHandler ---
    @Override
    public int receiveGas(int type, int amountMB) {
        if (gasType != Retronism_GasType.NONE && gasType != type) return 0;
        int accepted = Math.min(amountMB, gasCapacity - gasAmount);
        if (accepted > 0) { gasType = type; gasAmount += accepted; }
        return accepted;
    }

    @Override
    public int extractGas(int type, int amountMB) {
        if (gasType != type) return 0;
        int extracted = Math.min(amountMB, gasAmount);
        gasAmount -= extracted;
        if (gasAmount <= 0) { gasAmount = 0; gasType = Retronism_GasType.NONE; }
        return extracted;
    }

    @Override public int getGasType() { return gasType; }
    @Override public int getGasAmount() { return gasAmount; }
    @Override public int getGasCapacity() { return gasCapacity; }

    public int getCookProgressScaled(int scale) {
        return maxProcessTime > 0 ? processTime * scale / maxProcessTime : 0;
    }

    // --- NBT ---
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        isFormed = nbt.getBoolean("Formed");
        storedEnergy = nbt.getInteger("Energy");
        fluidType = nbt.getInteger("FluidType");
        fluidAmount = nbt.getInteger("FluidAmount");
        gasType = nbt.getInteger("GasType");
        gasAmount = nbt.getInteger("GasAmount");
        processTime = nbt.getShort("ProcessTime");

        NBTTagList items = nbt.getTagList("Items");
        inventory = new ItemStack[0];
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
        nbt.setInteger("FluidType", fluidType);
        nbt.setInteger("FluidAmount", fluidAmount);
        nbt.setInteger("GasType", gasType);
        nbt.setInteger("GasAmount", gasAmount);
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
