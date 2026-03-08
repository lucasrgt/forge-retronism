package retronism.tile;

import net.minecraft.src.*;
import retronism.api.Retronism_IFluidHandler;
import retronism.api.Retronism_FluidType;
import retronism.api.Retronism_IGasHandler;
import retronism.api.Retronism_GasType;
import retronism.api.Retronism_IEnergyReceiver;
import retronism.api.Retronism_PortRegistry;
import retronism.Retronism_Registry;

public class Retronism_TileOzonizer extends TileEntity implements IInventory, Retronism_IEnergyReceiver, Retronism_IFluidHandler, Retronism_IGasHandler {

    private ItemStack[] inventory = new ItemStack[0];
    public boolean isFormed = false;

    private int storedEnergy = 0;
    private int maxEnergy = 64000;

    private int fluidType = Retronism_FluidType.NONE;
    private int fluidAmount = 0;
    private int fluidCapacity = 8000;

    private int gasType = Retronism_GasType.NONE;
    private int gasAmount = 0;
    private int gasCapacity = 8000;

    public int processTime = 0;
    public int maxProcessTime = 200;
    private int energyPerTick = 32;

    private static final int TYPE_AIR = 0;
    private static final int TYPE_CONTROLLER = 1;
    private static final int TYPE_MACHINE_PORT = 2;
    private static final int TYPE_IRON_BLOCK = 3;
    private static final int TYPE_GLASS = 4;
    private static final int TYPE_COUNT = 5;

    private static final int[][][] STRUCTURE = {
        { // Layer 0
            {3, 3, 3},
            {3, 3, 3},
            {3, 3, 3},
        },
        { // Layer 1
            {3, 1, 3},
            {2, 0, 2},
            {3, 2, 3},
        },
        { // Layer 2
            {4, 3, 4},
            {3, 0, 3},
            {4, 3, 4},
        },
        { // Layer 3
            {0, 3, 0},
            {3, 2, 3},
            {0, 3, 0},
        },
    };


    // Port definitions: {structX, structY, structZ, portType, portMode}
    private static final int[][] PORTS = {
        {0, 1, 1, Retronism_PortRegistry.PORT_TYPE_ENERGY, Retronism_PortRegistry.PORT_MODE_INPUT},
        {2, 1, 1, Retronism_PortRegistry.PORT_TYPE_ENERGY, Retronism_PortRegistry.PORT_MODE_INPUT},
        {1, 1, 2, Retronism_PortRegistry.PORT_TYPE_FLUID, Retronism_PortRegistry.PORT_MODE_INPUT},
        {1, 3, 1, Retronism_PortRegistry.PORT_TYPE_GAS, Retronism_PortRegistry.PORT_MODE_OUTPUT},
    };

    private int formedRotation = -1;
    private boolean portsRegistered = false;

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
        expectedIds[TYPE_CONTROLLER] = Retronism_Registry.ozonizerControllerBlock.blockID;
        expectedIds[TYPE_MACHINE_PORT] = Retronism_Registry.machinePortBlock.blockID;
        expectedIds[TYPE_IRON_BLOCK] = 42;
        expectedIds[TYPE_GLASS] = 20;

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
                        if (blockId != expectedIds[expected]) {
                            allFails += "rot" + f + ":s(" + sx + "," + sy + "," + sz + ")w(" + wx + "," + wy + "," + wz + ")exp=" + expectedIds[expected] + "got=" + blockId + " | ";
                            ok = false;
                        }
                    }
                }
            }
            if (ok) {
                boolean wasFormed = isFormed;
                isFormed = true;
                formedRotation = f;
                if (!wasFormed) {
                    registerPorts(cx, cy, cz, facings[f]);
                    applyPortMetadata(world, cx, cy, cz, facings[f]);
                }
                return true;
            }
        }

        if (isFormed) {
            unregisterPorts(cx, cy, cz);
        }
        lastFailDebug = allFails;
        isFormed = false;
        formedRotation = -1;
        return false;
    }

    private void registerPorts(int cx, int cy, int cz, int[] facing) {
        int ctrlX = -1, ctrlZ = -1;
        for (int y = 0; y < STRUCTURE.length; y++)
            for (int z = 0; z < STRUCTURE[y].length; z++)
                for (int x = 0; x < STRUCTURE[y][z].length; x++)
                    if (STRUCTURE[y][z][x] == TYPE_CONTROLLER) { ctrlX = x; ctrlZ = z; }
        for (int[] port : PORTS) {
            int relX = port[0] - ctrlX, relZ = port[2] - ctrlZ;
            int wx = cx + relX * facing[0] + relZ * facing[2];
            int wy = cy + (port[1] - 1);
            int wz = cz + relX * facing[1] + relZ * facing[3];
            Retronism_PortRegistry.registerPort(wx, wy, wz, cx, cy, cz, port[3], port[4]);
        }
        portsRegistered = true;
    }

    private void unregisterPorts(int cx, int cy, int cz) {
        Retronism_PortRegistry.unregisterAllForController(cx, cy, cz);
        portsRegistered = false;
    }

    private void applyPortMetadata(World world, int cx, int cy, int cz, int[] facing) {
        int ctrlX = -1, ctrlZ = -1, ctrlY = -1;
        for (int y = 0; y < STRUCTURE.length; y++)
            for (int z = 0; z < STRUCTURE[y].length; z++)
                for (int x = 0; x < STRUCTURE[y][z].length; x++)
                    if (STRUCTURE[y][z][x] == TYPE_CONTROLLER) { ctrlX = x; ctrlY = y; ctrlZ = z; }
        for (int[] port : PORTS) {
            int relX = port[0] - ctrlX, relZ = port[2] - ctrlZ;
            int wx = cx + relX * facing[0] + relZ * facing[2];
            int wy = cy + (port[1] - ctrlY);
            int wz = cz + relX * facing[1] + relZ * facing[3];
            int meta = port[3] - 1; // PORT_TYPE_ENERGY=1->0, FLUID=2->1, GAS=3->2
            world.setBlockMetadataWithNotify(wx, wy, wz, meta);
        }
    }

    private int recheckTimer = 0;

    @Override
    public void updateEntity() {
        if (worldObj.multiplayerWorld) return;

        // Re-register ports after world load
        if (isFormed && !portsRegistered && formedRotation >= 0) {
            int[][] facings = {
                { 1, 0, 0, 1}, { 0, 1, -1, 0}, {-1, 0, 0,-1}, { 0,-1, 1, 0},
            };
            registerPorts(xCoord, yCoord, zCoord, facings[formedRotation]);
        }

        // Periodically recheck structure integrity
        if (isFormed && ++recheckTimer >= 20) {
            recheckTimer = 0;
            checkStructure(worldObj, xCoord, yCoord, zCoord);
        }

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
        if (inventory.length == 0 || inventory[0] == null) return false;
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
    public String getInvName() { return "Ozonizer"; }

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
        formedRotation = nbt.getInteger("FormedRotation");
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
        nbt.setInteger("FormedRotation", formedRotation);
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
