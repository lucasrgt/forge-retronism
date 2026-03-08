package retronism.container;

import net.minecraft.src.*;
import retronism.tile.Retronism_TileOzonizer;

public class Retronism_ContainerOzonizer extends Container {

    private Retronism_TileOzonizer tile;
    private int lastEnergy = -1;
    private int lastProcessTime = -1;
    private int lastMaxProcessTime = -1;

    public Retronism_ContainerOzonizer(InventoryPlayer playerInv, Retronism_TileOzonizer tile) {
        this.tile = tile;

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

    public boolean isUsableByPlayer(EntityPlayer player) {
        return tile.canInteractWith(player);
    }

    public void updateCraftingResults() {
        super.updateCraftingResults();
        for (int j = 0; j < this.field_20121_g.size(); j++) {
            ICrafting crafter = (ICrafting) this.field_20121_g.get(j);
            if (lastEnergy != tile.getStoredEnergy()) crafter.func_20158_a(this, 0, tile.getStoredEnergy());
            if (lastProcessTime != tile.processTime) crafter.func_20158_a(this, 1, tile.processTime);
            if (lastMaxProcessTime != tile.maxProcessTime) crafter.func_20158_a(this, 2, tile.maxProcessTime);
        }
        lastEnergy = tile.getStoredEnergy();
        lastProcessTime = tile.processTime;
        lastMaxProcessTime = tile.maxProcessTime;
    }

    public void func_20112_a(int id, int value) {
        if (id == 0) lastEnergy = value;
        if (id == 1) tile.processTime = value;
        if (id == 2) tile.maxProcessTime = value;
    }

    public ItemStack getStackInSlot(int slotIndex) {
        ItemStack result = null;
        Slot slot = (Slot) this.slots.get(slotIndex);
        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            result = slotStack.copy();
            if (slotIndex < 27) {
                this.func_28125_a(slotStack, 27, 36, false);
            } else {
                this.func_28125_a(slotStack, 0, 27, false);
            }
            if (slotStack.stackSize == 0) slot.putStack(null);
            else slot.onSlotChanged();
        }
        return result;
    }
}
