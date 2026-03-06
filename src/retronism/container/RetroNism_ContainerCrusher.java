package retronism.container;

import net.minecraft.src.*;
import retronism.tile.*;
import retronism.slot.*;

public class Retronism_ContainerCrusher extends Container {
	private Retronism_TileCrusher crusher;
	private int lastCookTime = 0;
	private int lastEnergy = 0;

	public Retronism_ContainerCrusher(InventoryPlayer playerInv, Retronism_TileCrusher crusher) {
		this.crusher = crusher;
		// Input slot (left)
		this.addSlot(new Slot(crusher, 0, 56, 35));
		// Output slot (right)
		this.addSlot(new Retronism_SlotCrusher(playerInv.player, crusher, 1, 116, 35));

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
			if (this.lastCookTime != this.crusher.crusherCookTime) {
				crafter.func_20158_a(this, 0, this.crusher.crusherCookTime);
			}
			if (this.lastEnergy != this.crusher.storedEnergy) {
				crafter.func_20158_a(this, 1, this.crusher.storedEnergy);
			}
		}
		this.lastCookTime = this.crusher.crusherCookTime;
		this.lastEnergy = this.crusher.storedEnergy;
	}

	public void func_20112_a(int id, int value) {
		if (id == 0) this.crusher.crusherCookTime = value;
		if (id == 1) this.crusher.storedEnergy = value;
	}

	public boolean isUsableByPlayer(EntityPlayer player) {
		return this.crusher.canInteractWith(player);
	}

	public ItemStack getStackInSlot(int slotIndex) {
		ItemStack result = null;
		Slot slot = (Slot) this.slots.get(slotIndex);
		if (slot != null && slot.getHasStack()) {
			ItemStack slotStack = slot.getStack();
			result = slotStack.copy();
			if (slotIndex == 1) {
				this.func_28125_a(slotStack, 2, 38, true);
			} else if (slotIndex >= 2 && slotIndex < 29) {
				this.func_28125_a(slotStack, 29, 38, false);
			} else if (slotIndex >= 29 && slotIndex < 38) {
				this.func_28125_a(slotStack, 2, 29, false);
			} else {
				this.func_28125_a(slotStack, 2, 38, false);
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
