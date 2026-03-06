package retronism.container;

import net.minecraft.src.*;
import retronism.tile.*;

public class Retronism_ContainerGenerator extends Container {
	private Retronism_TileGenerator generator;
	private int lastBurnTime = 0;
	private int lastItemBurnTime = 0;
	private int lastEnergy = 0;
	private int lastOutput = 0;

	public Retronism_ContainerGenerator(InventoryPlayer playerInv, Retronism_TileGenerator generator) {
		this.generator = generator;
		// Fuel slot (matches texture inner area at 80,35)
		this.addSlot(new Slot(generator, 0, 80, 35));

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
			if (this.lastBurnTime != this.generator.burnTime) {
				crafter.func_20158_a(this, 0, this.generator.burnTime);
			}
			if (this.lastItemBurnTime != this.generator.currentItemBurnTime) {
				crafter.func_20158_a(this, 1, this.generator.currentItemBurnTime);
			}
			if (this.lastEnergy != this.generator.storedEnergy) {
				crafter.func_20158_a(this, 2, this.generator.storedEnergy);
			}
			if (this.lastOutput != this.generator.lastOutput) {
				crafter.func_20158_a(this, 3, this.generator.lastOutput);
			}
		}
		this.lastBurnTime = this.generator.burnTime;
		this.lastItemBurnTime = this.generator.currentItemBurnTime;
		this.lastEnergy = this.generator.storedEnergy;
		this.lastOutput = this.generator.lastOutput;
	}

	public void func_20112_a(int id, int value) {
		if (id == 0) this.generator.burnTime = value;
		if (id == 1) this.generator.currentItemBurnTime = value;
		if (id == 2) this.generator.storedEnergy = value;
		if (id == 3) this.generator.lastOutput = value;
	}

	public boolean isUsableByPlayer(EntityPlayer player) {
		return this.generator.canInteractWith(player);
	}

	public ItemStack getStackInSlot(int slotIndex) {
		ItemStack result = null;
		Slot slot = (Slot) this.slots.get(slotIndex);
		if (slot != null && slot.getHasStack()) {
			ItemStack slotStack = slot.getStack();
			result = slotStack.copy();
			if (slotIndex == 0) {
				this.func_28125_a(slotStack, 1, 37, true);
			} else if (slotIndex >= 1 && slotIndex < 28) {
				this.func_28125_a(slotStack, 28, 37, false);
			} else if (slotIndex >= 28 && slotIndex < 37) {
				this.func_28125_a(slotStack, 1, 28, false);
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
