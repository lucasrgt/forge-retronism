import { GeneratedFile, SerializedMultiblock, GuiComponent, blockRegistry } from './types.js';
import { getState, getSlotInfo, getComponentByType, serialize } from './state.js';

const PREFIX = 'Retronism_';

export function generateAllFiles(): GeneratedFile[] {
  const json = serialize();
  const state = getState();
  const name = json.name;
  const files: GeneratedFile[] = [];

  const hasEnergy = json.ioTypes.includes('energy');
  const hasFluid = json.ioTypes.includes('fluid');
  const hasGas = json.ioTypes.includes('gas');
  const hasItem = json.ioTypes.includes('item');
  const slotInfo = getSlotInfo();
  const totalSlots = slotInfo.inputs.length + slotInfo.outputs.length;

  // 0. JSON definition
  files.push({
    name: `${name}.json`,
    relativePath: `tools/multiblock-designer/output/${name}.json`,
    content: JSON.stringify(json, null, 2),
  });

  // 1. Casing Block
  files.push({
    name: `${PREFIX}Block${name}Casing.java`,
    relativePath: `src/retronism/block/${PREFIX}Block${name}Casing.java`,
    content: genCasingBlock(name),
  });

  // 2. Controller Block
  files.push({
    name: `${PREFIX}Block${name}Controller.java`,
    relativePath: `src/retronism/block/${PREFIX}Block${name}Controller.java`,
    content: genControllerBlock(name),
  });

  // 3. Tile Entity
  files.push({
    name: `${PREFIX}Tile${name}.java`,
    relativePath: `src/retronism/tile/${PREFIX}Tile${name}.java`,
    content: genTileEntity(json, name, hasEnergy, hasFluid, hasGas, hasItem, totalSlots, slotInfo),
  });

  // 4. Container
  files.push({
    name: `${PREFIX}Container${name}.java`,
    relativePath: `src/retronism/container/${PREFIX}Container${name}.java`,
    content: genContainer(json, name, totalSlots, slotInfo),
  });

  // 5. GUI
  files.push({
    name: `${PREFIX}Gui${name}.java`,
    relativePath: `src/retronism/gui/${PREFIX}Gui${name}.java`,
    content: genGui(json, name, hasEnergy, hasFluid, hasGas),
  });

  // 6. Output Slot
  if (slotInfo.outputs.length > 0) {
    files.push({
      name: `${PREFIX}Slot${name}Output.java`,
      relativePath: `src/retronism/slot/${PREFIX}Slot${name}Output.java`,
      content: genOutputSlot(name),
    });
  }

  // 7. GUI Builder Script
  files.push({
    name: `build_gui_${name.toLowerCase()}.py`,
    relativePath: `tools/build_gui_${name.toLowerCase()}.py`,
    content: genGuiBuilderScript(name, state.guiComponents),
  });

  // 8. Blockbench model (if linked)
  if (state.model) {
    files.push({
      name: `${name}.model.json`,
      relativePath: `src/retronism/assets/models/${name}.model.json`,
      content: JSON.stringify({
        name: state.model.name,
        elements: state.model.elements,
        textureName: state.model.textureName,
      }, null, 2),
    });
  }

  return files;
}

// ---------------------------------------------------------------------------
// Individual generators
// ---------------------------------------------------------------------------

function genCasingBlock(name: string): string {
  return `package retronism.block;

import net.minecraft.src.*;

public class ${PREFIX}Block${name}Casing extends Block {
    public ${PREFIX}Block${name}Casing(int id, int tex) {
        super(id, tex, Material.iron);
        setHardness(3.5F);
        setResistance(5.0F);
        setStepSound(Block.soundMetalFootstep);
        setBlockName("${name.toLowerCase()}Casing");
    }
}
`;
}

function genControllerBlock(name: string): string {
  return `package retronism.block;

import net.minecraft.src.*;

public class ${PREFIX}Block${name}Controller extends BlockContainer {
    public ${PREFIX}Block${name}Controller(int id, int tex) {
        super(id, tex, Material.iron);
        setHardness(3.5F);
        setResistance(5.0F);
        setStepSound(Block.soundMetalFootstep);
        setBlockName("${name.toLowerCase()}Controller");
    }

    @Override
    protected TileEntity getBlockEntity() {
        return new ${PREFIX}Tile${name}();
    }

    @Override
    public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
        if (world.multiplayerWorld) return true;
        ${PREFIX}Tile${name} tile = (${PREFIX}Tile${name}) world.getBlockTileEntity(x, y, z);
        if (tile == null) return false;

        if (!tile.checkStructure(world, x, y, z)) {
            return true;
        }

        ModLoader.OpenGUI(player, new ${PREFIX}Gui${name}(player.inventory, tile));
        return true;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, int neighborId) {
        ${PREFIX}Tile${name} tile = (${PREFIX}Tile${name}) world.getBlockTileEntity(x, y, z);
        if (tile != null) {
            tile.checkStructure(world, x, y, z);
        }
    }

    @Override
    public void onBlockRemoval(World world, int x, int y, int z) {
        ${PREFIX}Tile${name} tile = (${PREFIX}Tile${name}) world.getBlockTileEntity(x, y, z);
        if (tile != null) {
            for (int i = 0; i < tile.getSizeInventory(); i++) {
                ItemStack stack = tile.getStackInSlot(i);
                if (stack != null) {
                    float rx = world.rand.nextFloat() * 0.6F + 0.1F;
                    float ry = world.rand.nextFloat() * 0.6F + 0.1F;
                    float rz = world.rand.nextFloat() * 0.6F + 0.1F;
                    EntityItem entity = new EntityItem(world, x + rx, y + ry, z + rz, stack);
                    world.entityJoinedWorld(entity);
                }
            }
        }
        super.onBlockRemoval(world, x, y, z);
    }
}
`;
}

function genTileEntity(
  json: SerializedMultiblock, name: string,
  hasEnergy: boolean, hasFluid: boolean, hasGas: boolean, hasItem: boolean,
  totalSlots: number, slotInfo: ReturnType<typeof getSlotInfo>,
): string {
  const interfaces = ['IInventory'];
  if (hasEnergy) interfaces.push('Retronism_IEnergyReceiver');
  if (hasFluid) interfaces.push('Retronism_IFluidHandler');
  if (hasGas) interfaces.push('Retronism_IGasHandler');

  const inputSlots = slotInfo.inputs.length;
  const outputSlots = slotInfo.outputs.length;

  let code = `package retronism.tile;

import net.minecraft.src.*;
${hasFluid ? 'import retronism.api.Retronism_IFluidHandler;\nimport retronism.api.Retronism_FluidType;\n' : ''}${hasGas ? 'import retronism.api.Retronism_IGasHandler;\nimport retronism.api.Retronism_GasType;\n' : ''}${hasEnergy ? 'import retronism.api.Retronism_IEnergyReceiver;\n' : ''}
public class ${PREFIX}Tile${name} extends TileEntity implements ${interfaces.join(', ')} {

    private ItemStack[] inventory = new ItemStack[${totalSlots}];
    public boolean isFormed = false;
`;

  if (hasEnergy) {
    code += `
    private int storedEnergy = 0;
    private int maxEnergy = ${json.capacity.energy};
`;
  }

  if (hasFluid) {
    code += `
    private int fluidType = Retronism_FluidType.NONE;
    private int fluidAmount = 0;
    private int fluidCapacity = ${json.capacity.fluid};
`;
  }

  if (hasGas) {
    code += `
    private int gasType = Retronism_GasType.NONE;
    private int gasAmount = 0;
    private int gasCapacity = ${json.capacity.gas};
`;
  }

  if (json.structType === 'machine') {
    code += `
    public int processTime = 0;
    public int maxProcessTime = ${json.processTime};
    private int energyPerTick = ${json.energyPerTick};
`;
  }

  // Structure pattern
  const structResult = genStructureArray(json);
  code += `
${structResult.code}

    public boolean checkStructure(World world, int cx, int cy, int cz) {
        int ctrlX = -1, ctrlY = -1, ctrlZ = -1;
        for (int y = 0; y < STRUCTURE.length; y++) {
            for (int z = 0; z < STRUCTURE[y].length; z++) {
                for (int x = 0; x < STRUCTURE[y][z].length; x++) {
                    if (STRUCTURE[y][z][x] == ${structResult.controllerConst}) {
                        ctrlX = x; ctrlY = y; ctrlZ = z;
                    }
                }
            }
        }
        if (ctrlX == -1) { isFormed = false; return false; }

        int casingId = mod_Retronism.block${name}Casing.blockID;
        int controllerId = mod_Retronism.block${name}Controller.blockID;

        for (int y = 0; y < STRUCTURE.length; y++) {
            for (int z = 0; z < STRUCTURE[y].length; z++) {
                for (int x = 0; x < STRUCTURE[y][z].length; x++) {
                    int expected = STRUCTURE[y][z][x];
                    if (expected == TYPE_AIR) continue;

                    int wx = cx + (x - ctrlX);
                    int wy = cy + (y - ctrlY);
                    int wz = cz + (z - ctrlZ);
                    int blockId = world.getBlockId(wx, wy, wz);

                    if (expected == ${structResult.controllerConst}) {
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
`;

  // updateEntity for machines
  if (json.structType === 'machine') {
    code += `
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
`;
  }

  // IInventory
  code += `
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
    public String getInvName() { return "${name}"; }

    @Override
    public int getInventoryStackLimit() { return 64; }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this
            && player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64.0;
    }
`;

  // Energy
  if (hasEnergy) {
    code += `
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
`;
  }

  // Fluid
  if (hasFluid) {
    code += `
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
`;
  }

  // Gas
  if (hasGas) {
    code += `
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
`;
  }

  // Scaling helpers
  if (json.structType === 'machine') {
    code += `
    public int getCookProgressScaled(int scale) {
        return maxProcessTime > 0 ? processTime * scale / maxProcessTime : 0;
    }
`;
  }

  // NBT
  code += `
    // --- NBT ---
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        isFormed = nbt.getBoolean("Formed");
`;
  if (hasEnergy) code += `        storedEnergy = nbt.getInteger("Energy");\n`;
  if (hasFluid) {
    code += `        fluidType = nbt.getInteger("FluidType");\n`;
    code += `        fluidAmount = nbt.getInteger("FluidAmount");\n`;
  }
  if (hasGas) {
    code += `        gasType = nbt.getInteger("GasType");\n`;
    code += `        gasAmount = nbt.getInteger("GasAmount");\n`;
  }
  if (json.structType === 'machine') {
    code += `        processTime = nbt.getShort("ProcessTime");\n`;
  }
  code += `
        NBTTagList items = nbt.getTagList("Items");
        inventory = new ItemStack[${totalSlots}];
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
`;
  if (hasEnergy) code += `        nbt.setInteger("Energy", storedEnergy);\n`;
  if (hasFluid) {
    code += `        nbt.setInteger("FluidType", fluidType);\n`;
    code += `        nbt.setInteger("FluidAmount", fluidAmount);\n`;
  }
  if (hasGas) {
    code += `        nbt.setInteger("GasType", gasType);\n`;
    code += `        nbt.setInteger("GasAmount", gasAmount);\n`;
  }
  if (json.structType === 'machine') {
    code += `        nbt.setShort("ProcessTime", (short) processTime);\n`;
  }
  code += `
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
`;

  return code;
}

interface StructureArrayResult {
  code: string;
  controllerConst: string;
}

function genStructureArray(json: SerializedMultiblock): StructureArrayResult {
  // Collect all chars used in the structure
  const usedChars = new Set<string>();
  for (const layer of json.structure) {
    for (const row of layer.pattern) {
      for (const ch of row) {
        if (ch !== ' ') usedChars.add(ch);
      }
    }
  }

  // Build char-to-int mapping dynamically from registry
  const charToInt: Record<string, number> = { ' ': 0 };
  let nextInt = 1;
  let code = '    private static final int TYPE_AIR = 0;\n';
  let controllerConst = 'TYPE_CONTROLLER'; // fallback

  for (const ch of usedChars) {
    const blockDef = blockRegistry.getByChar(ch);
    if (blockDef) {
      charToInt[ch] = nextInt;
      const constName = `TYPE_${blockDef.id.toUpperCase()}`;
      code += `    private static final int ${constName} = ${nextInt};\n`;
      if (blockDef.category === 'controller') {
        controllerConst = constName;
      }
      nextInt++;
    }
  }

  code += '\n    private static final int[][][] STRUCTURE = {\n';
  for (const layer of json.structure) {
    code += `        { // Layer ${layer.layer}\n`;
    for (const row of layer.pattern) {
      const ints = row.map(ch => charToInt[ch] || 0);
      code += `            {${ints.join(', ')}},\n`;
    }
    code += '        },\n';
  }
  code += '    };\n';

  return { code, controllerConst };
}

function genContainer(
  json: SerializedMultiblock, name: string, totalSlots: number,
  slotInfo: ReturnType<typeof getSlotInfo>,
): string {
  const inputSlots = slotInfo.inputs.length;
  const outputSlots = slotInfo.outputs.length;

  let slotCode = '';
  let slotIdx = 0;
  for (const slot of slotInfo.inputs) {
    slotCode += `        addSlot(new Slot(tile, ${slotIdx}, ${slot.x}, ${slot.y}));\n`;
    slotIdx++;
  }
  for (const slot of slotInfo.outputs) {
    slotCode += `        addSlot(new ${PREFIX}Slot${name}Output(tile, ${slotIdx}, ${slot.x}, ${slot.y}));\n`;
    slotIdx++;
  }

  const syncFields: { field: string; getter: string }[] = [];
  if (json.ioTypes.includes('energy')) {
    syncFields.push({ field: 'lastEnergy', getter: 'tile.getStoredEnergy()' });
  }
  if (json.structType === 'machine') {
    syncFields.push({ field: 'lastProcessTime', getter: 'tile.processTime' });
    syncFields.push({ field: 'lastMaxProcessTime', getter: 'tile.maxProcessTime' });
  }

  const lastFieldDecl = syncFields.map(f => `    private int ${f.field} = -1;`).join('\n');
  const syncCheck = syncFields.map((f, i) =>
    `        if (${f.field} != ${f.getter}) {\n            ${f.field} = ${f.getter};\n            for (int j = 0; j < crafters.size(); j++) {\n                ((ICrafting)crafters.get(j)).updateCraftingInventoryInfo(this, ${i}, ${f.field});\n            }\n        }`
  ).join('\n');
  const syncReceive = syncFields.map((f, i) =>
    `        if (id == ${i}) { /* ${f.field} */ }`
  ).join('\n');

  return `package retronism.container;

import net.minecraft.src.*;
${outputSlots > 0 ? `import retronism.slot.${PREFIX}Slot${name}Output;\n` : ''}import retronism.tile.${PREFIX}Tile${name};

public class ${PREFIX}Container${name} extends Container {

    private ${PREFIX}Tile${name} tile;
${lastFieldDecl}

    public ${PREFIX}Container${name}(InventoryPlayer playerInv, ${PREFIX}Tile${name} tile) {
        this.tile = tile;

        // Machine slots
${slotCode}
        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tile.canInteractWith(player);
    }

    @Override
    public void updateCraftingResults() {
        super.updateCraftingResults();
${syncCheck}
    }

    @Override
    public void func_20112_a(int id, int value) {
${syncReceive}
    }

    @Override
    public ItemStack getStackInSlot(int slotIndex) {
        ItemStack result = null;
        Slot slot = (Slot) inventorySlots.get(slotIndex);
        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            result = slotStack.copy();
            if (slotIndex < ${totalSlots}) {
                if (!func_28125_a(slotStack, ${totalSlots}, ${totalSlots + 36}, true)) return null;
            } else {
                if (!func_28125_a(slotStack, 0, ${inputSlots}, false)) return null;
            }
            if (slotStack.stackSize == 0) slot.putStack(null);
            else slot.onSlotChanged();
        }
        return result;
    }
}
`;
}

function genGui(
  json: SerializedMultiblock, name: string,
  hasEnergy: boolean, hasFluid: boolean, hasGas: boolean,
): string {
  const textureName = `retronism_${name.toLowerCase()}`;
  let overlayCode = '';

  const energyBar = getComponentByType('energy_bar');
  if (hasEnergy && energyBar) {
    const bx = energyBar.x + 1, by = energyBar.y + 1;
    const bw = energyBar.w - 2, bh = energyBar.h - 2;
    overlayCode += `
        // Energy bar fill
        int barX = x + ${bx}, barY = y + ${by}, barW = ${bw}, barH = ${bh};
        int scaled = tile.getEnergyScaled(barH);
        if (scaled > 0) {
            int top = barY + barH - scaled;
            for (int sy = top; sy < barY + barH; sy++) {
                int color = (sy % 2 == 0) ? 0xFF3BFB98 : 0xFF36E38A;
                drawRect(barX, sy, barX + barW, sy + 1, color);
            }
        }
`;
  }

  const arrow = getComponentByType('progress_arrow');
  if (json.structType === 'machine' && arrow) {
    overlayCode += `
        // Progress arrow
        int cookScale = tile.getCookProgressScaled(24);
        if (cookScale > 0) {
            this.drawTexturedModalRect(x + ${arrow.x}, y + ${arrow.y}, 176, 14, cookScale + 1, 17);
        }
`;
  }

  const fluidTank = getComponentByType('fluid_tank');
  if (hasFluid && fluidTank) {
    const tx = fluidTank.x + 1, ty = fluidTank.y + 1;
    const tw = fluidTank.w - 2, th = fluidTank.h - 2;
    overlayCode += `
        // Fluid tank fill
        int fluidScaled = tile.getFluidAmount() * ${th} / Math.max(1, tile.getFluidCapacity());
        if (fluidScaled > 0) {
            drawRect(x + ${tx}, y + ${ty} + ${th} - fluidScaled, x + ${tx} + ${tw}, y + ${ty} + ${th}, 0xFF2850DC);
        }
`;
  }

  const gasTank = getComponentByType('gas_tank');
  if (hasGas && gasTank) {
    const gx = gasTank.x + 1, gy = gasTank.y + 1;
    const gw = gasTank.w - 2, gh = gasTank.h - 2;
    overlayCode += `
        // Gas tank fill
        int gasScaled = tile.getGasAmount() * ${gh} / Math.max(1, tile.getGasCapacity());
        if (gasScaled > 0) {
            drawRect(x + ${gx}, y + ${gy} + ${gh} - gasScaled, x + ${gx} + ${gw}, y + ${gy} + ${gh}, 0xFFAAAAAA);
        }
`;
  }

  return `package retronism.gui;

import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;
import retronism.tile.${PREFIX}Tile${name};
import retronism.container.${PREFIX}Container${name};

public class ${PREFIX}Gui${name} extends GuiContainer {

    private ${PREFIX}Tile${name} tile;
    private int textureID;

    public ${PREFIX}Gui${name}(InventoryPlayer playerInv, ${PREFIX}Tile${name} tile) {
        super(new ${PREFIX}Container${name}(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    protected void drawGuiContainerForegroundLayer() {
        fontRenderer.drawString("${name}", (xSize - fontRenderer.getStringWidth("${name}")) / 2, 6, 4210752);
        fontRenderer.drawString("Inventory", 8, ySize - 96 + 2, 4210752);

        if (!tile.isFormed) {
            fontRenderer.drawString("Structure incomplete!", 8, 20, 0xFF4444);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        textureID = this.mc.renderEngine.getTexture("/gui/${textureName}.png");
        this.mc.renderEngine.bindTexture(textureID);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
${overlayCode}
    }
}
`;
}

function genOutputSlot(name: string): string {
  return `package retronism.slot;

import net.minecraft.src.*;

public class ${PREFIX}Slot${name}Output extends Slot {
    public ${PREFIX}Slot${name}Output(IInventory inv, int slotIndex, int x, int y) {
        super(inv, slotIndex, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }
}
`;
}

function genGuiBuilderScript(name: string, components: GuiComponent[]): string {
  let script = `import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__)))
from gui_builder import GuiBuilder

gui = GuiBuilder()
gui.panel(0, 0, 176, 166)
`;
  for (const comp of components) {
    switch (comp.type) {
      case 'slot':
        script += `gui.slot(${comp.x}, ${comp.y})\n`;
        break;
      case 'big_slot':
        script += `gui.big_slot(${comp.x}, ${comp.y})\n`;
        break;
      case 'energy_bar':
        script += `gui.energy_bar(${comp.x}, ${comp.y}, ${comp.w}, ${comp.h})\n`;
        break;
      case 'progress_arrow':
        script += `gui.progress_arrow(${comp.x}, ${comp.y})\n`;
        break;
      case 'flame':
        script += `gui.flame(${comp.x}, ${comp.y})\n`;
        break;
      case 'fluid_tank':
        script += `gui.energy_bar(${comp.x}, ${comp.y}, ${comp.w}, ${comp.h})  # fluid tank\n`;
        break;
      case 'gas_tank':
        script += `gui.energy_bar(${comp.x}, ${comp.y}, ${comp.w}, ${comp.h})  # gas tank\n`;
        break;
      case 'separator':
        script += `gui.separator(${comp.x}, ${comp.y}, ${comp.w})\n`;
        break;
    }
  }
  script += `gui.player_inventory(7, 83)\n`;
  script += `\ngui.save(os.path.join(os.path.dirname(__file__), "..", "temp", "merged", "gui", "retronism_${name.toLowerCase()}.png"))\n`;
  script += `print("Generated GUI texture for ${name}")\n`;
  return script;
}
