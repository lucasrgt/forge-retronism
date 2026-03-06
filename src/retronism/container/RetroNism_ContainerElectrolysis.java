package retronism.container;

import net.minecraft.src.*;
import retronism.tile.*;

public class Retronism_ContainerElectrolysis extends Container {
	private Retronism_TileElectrolysis tile;
	private int lastProcessTime = 0;
	private int lastEnergy = 0;
	private int lastWater = 0;
	private int lastHydrogen = 0;
	private int lastOxygen = 0;

	public Retronism_ContainerElectrolysis(InventoryPlayer playerInv, Retronism_TileElectrolysis tile) {
		this.tile = tile;

		// Player inventory
		int i;
		for (i = 0; i < 3; ++i) {
			for (int j = 0; j < 9; ++j) {
				this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
			}
		}
		for (i = 0; i < 9; ++i) {
			this.addSlot(new Slot(playerInv, i, 8 + i * 18, 142));
		}
	}

	public void updateCraftingResults() {
		super.updateCraftingResults();
		for (int i = 0; i < this.field_20121_g.size(); ++i) {
			ICrafting crafter = (ICrafting) this.field_20121_g.get(i);
			if (this.lastProcessTime != this.tile.processTime) {
				crafter.func_20158_a(this, 0, this.tile.processTime);
			}
			if (this.lastEnergy != this.tile.storedEnergy) {
				crafter.func_20158_a(this, 1, this.tile.storedEnergy);
			}
			if (this.lastWater != this.tile.waterStored) {
				crafter.func_20158_a(this, 2, this.tile.waterStored);
			}
			if (this.lastHydrogen != this.tile.hydrogenStored) {
				crafter.func_20158_a(this, 3, this.tile.hydrogenStored);
			}
			if (this.lastOxygen != this.tile.oxygenStored) {
				crafter.func_20158_a(this, 4, this.tile.oxygenStored);
			}
		}
		this.lastProcessTime = this.tile.processTime;
		this.lastEnergy = this.tile.storedEnergy;
		this.lastWater = this.tile.waterStored;
		this.lastHydrogen = this.tile.hydrogenStored;
		this.lastOxygen = this.tile.oxygenStored;
	}

	public void func_20112_a(int id, int value) {
		if (id == 0) this.tile.processTime = value;
		if (id == 1) this.tile.storedEnergy = value;
		if (id == 2) this.tile.waterStored = value;
		if (id == 3) this.tile.hydrogenStored = value;
		if (id == 4) this.tile.oxygenStored = value;
	}

	public boolean isUsableByPlayer(EntityPlayer player) {
		return this.tile.canInteractWith(player);
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

			if (slotStack.stackSize == 0) {
				slot.putStack((ItemStack) null);
			} else {
				slot.onSlotChanged();
			}
			if (slotStack.stackSize == result.stackSize) return null;
			slot.onPickupFromSlot(slotStack);
		}
		return result;
	}
}
