package retronism.container;

import net.minecraft.src.*;
import retronism.*;
import retronism.tile.*;
import retronism.slot.*;

public class Retronism_ContainerGasTank extends Container {
	private Retronism_TileGasTank tank;
	private int lastGasType = 0;
	private int lastGasAmount = 0;

	public Retronism_ContainerGasTank(InventoryPlayer playerInv, Retronism_TileGasTank tank) {
		this.tank = tank;
		this.addSlot(new Retronism_SlotGasTankCell(tank, 0, 80, 35));

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
			if (this.lastGasType != this.tank.getGasType()) {
				crafter.func_20158_a(this, 0, this.tank.getGasType());
			}
			if (this.lastGasAmount != this.tank.getGasAmount()) {
				crafter.func_20158_a(this, 1, this.tank.getGasAmount());
			}
		}
		this.lastGasType = this.tank.getGasType();
		this.lastGasAmount = this.tank.getGasAmount();
	}

	public void func_20112_a(int id, int value) {
		if (id == 0) this.tank.setGasClient(value, this.tank.getGasAmount());
		if (id == 1) this.tank.setGasClient(this.tank.getGasType(), value);
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
			} else if (slotStack.itemID == Retronism_Registry.gasCellEmpty.shiftedIndex) {
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
