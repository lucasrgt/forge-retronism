package retronism.container;

import net.minecraft.src.*;
import retronism.tile.Retronism_TileMegaElectrolysis;

public class Retronism_ContainerMegaElectrolysis extends Container {

    private Retronism_TileMegaElectrolysis tile;
    private int lastEnergy = -1;
    private int lastProcessTime = -1;
    private int lastMaxProcessTime = -1;

    public Retronism_ContainerMegaElectrolysis(InventoryPlayer playerInv, Retronism_TileMegaElectrolysis tile) {
        this.tile = tile;

        // Machine slots

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
        if (lastEnergy != tile.getStoredEnergy()) {
            lastEnergy = tile.getStoredEnergy();
            for (int j = 0; j < crafters.size(); j++) {
                ((ICrafting)crafters.get(j)).updateCraftingInventoryInfo(this, 0, lastEnergy);
            }
        }
        if (lastProcessTime != tile.processTime) {
            lastProcessTime = tile.processTime;
            for (int j = 0; j < crafters.size(); j++) {
                ((ICrafting)crafters.get(j)).updateCraftingInventoryInfo(this, 1, lastProcessTime);
            }
        }
        if (lastMaxProcessTime != tile.maxProcessTime) {
            lastMaxProcessTime = tile.maxProcessTime;
            for (int j = 0; j < crafters.size(); j++) {
                ((ICrafting)crafters.get(j)).updateCraftingInventoryInfo(this, 2, lastMaxProcessTime);
            }
        }
    }

    @Override
    public void func_20112_a(int id, int value) {
        if (id == 0) { /* lastEnergy */ }
        if (id == 1) { /* lastProcessTime */ }
        if (id == 2) { /* lastMaxProcessTime */ }
    }

    @Override
    public ItemStack getStackInSlot(int slotIndex) {
        ItemStack result = null;
        Slot slot = (Slot) inventorySlots.get(slotIndex);
        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            result = slotStack.copy();
            if (slotIndex < 0) {
                if (!func_28125_a(slotStack, 0, 36, true)) return null;
            } else {
                if (!func_28125_a(slotStack, 0, 0, false)) return null;
            }
            if (slotStack.stackSize == 0) slot.putStack(null);
            else slot.onSlotChanged();
        }
        return result;
    }
}
