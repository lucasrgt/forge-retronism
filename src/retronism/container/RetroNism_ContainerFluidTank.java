package retronism.container;

import net.minecraft.src.*;
import retronism.tile.*;
import retronism.slot.*;

public class Retronism_ContainerFluidTank extends Container {
	private Retronism_TileFluidTank tank;
	private int lastFluid = 0;

	public Retronism_ContainerFluidTank(InventoryPlayer playerInv, Retronism_TileFluidTank tank) {
		this.tank = tank;
		this.addSlot(new Retronism_SlotFluidTankBucket(tank, 0, 80, 35));

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
			if (this.lastFluid != this.tank.getFluidAmount()) {
				crafter.func_20158_a(this, 0, this.tank.getFluidAmount());
			}
		}
		this.lastFluid = this.tank.getFluidAmount();
	}

	public void func_20112_a(int id, int value) {
		if (id == 0) this.tank.setFluidAmountClient(value);
	}

	public boolean isUsableByPlayer(EntityPlayer player) {
		return this.tank.canInteractWith(player);
	}

	public ItemStack getStackInSlot(int slotIndex) {
		ItemStack result = null;
		Slot slot = (Slot) this.slots.get(slotIndex);
		if (slot != null && slot.getHasStack()) {
			ItemStack slotStack = slot.getStack();
			result = slotStack.copy();
			if (slotIndex == 0) {
				this.func_28125_a(slotStack, 1, 37, true);
			} else if (slotStack.itemID == Item.bucketEmpty.shiftedIndex) {
				this.func_28125_a(slotStack, 0, 1, false);
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
