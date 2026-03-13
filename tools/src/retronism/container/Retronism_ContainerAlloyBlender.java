package retronism.container;

import net.minecraft.src.*;
import retronism.slot.Retronism_SlotAlloyBlenderOutput;
import retronism.tile.Retronism_TileAlloyBlender;

public class Retronism_ContainerAlloyBlender extends Container {

    private Retronism_TileAlloyBlender tile;
    private int lastEnergy = -1;
    private int lastProcessTime = -1;
    private int lastMaxProcessTime = -1;

    public Retronism_ContainerAlloyBlender(InventoryPlayer playerInv, Retronism_TileAlloyBlender tile) {
        this.tile = tile;

        // Machine slots
        addSlot(new Slot(tile, 0, 48, 18));
        addSlot(new Slot(tile, 1, 72, 18));
        addSlot(new Slot(tile, 2, 60, 54));
        addSlot(new Retronism_SlotAlloyBlenderOutput(tile, 3, 134, 31));

        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 7 + col * 18, 83 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 7 + col * 18, 141));
        }
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return tile.canInteractWith(player);
    }

    public void updateCraftingResults() {
        super.updateCraftingResults();
        if (lastEnergy != tile.getStoredEnergy()) {
            lastEnergy = tile.getStoredEnergy();
            for (int j = 0; j < this.field_20121_g.size(); j++) {
                ((ICrafting)this.field_20121_g.get(j)).func_20158_a(this, 0, lastEnergy);
            }
        }
        if (lastProcessTime != tile.processTime) {
            lastProcessTime = tile.processTime;
            for (int j = 0; j < this.field_20121_g.size(); j++) {
                ((ICrafting)this.field_20121_g.get(j)).func_20158_a(this, 1, lastProcessTime);
            }
        }
        if (lastMaxProcessTime != tile.maxProcessTime) {
            lastMaxProcessTime = tile.maxProcessTime;
            for (int j = 0; j < this.field_20121_g.size(); j++) {
                ((ICrafting)this.field_20121_g.get(j)).func_20158_a(this, 2, lastMaxProcessTime);
            }
        }
    }

    public void func_20112_a(int id, int value) {
        if (id == 0) { /* lastEnergy */ }
        if (id == 1) { /* lastProcessTime */ }
        if (id == 2) { /* lastMaxProcessTime */ }
    }

    public ItemStack getStackInSlot(int slotIndex) {
        ItemStack result = null;
        Slot slot = (Slot) this.slots.get(slotIndex);
        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            result = slotStack.copy();
            if (slotIndex < 4) {
                if (!func_28125_a(slotStack, 4, 40, true)) return null;
            } else {
                if (!func_28125_a(slotStack, 0, 3, false)) return null;
            }
            if (slotStack.stackSize == 0) slot.putStack(null);
            else slot.onSlotChanged();
        }
        return result;
    }
}
