package retronism.container;

import net.minecraft.src.*;
import retronism.tile.*;
import retronism.slot.*;

public class Retronism_ContainerMegaCrusher extends Container {
	private Retronism_TileMegaCrusher megaCrusher;
	private int lastCookTime0 = 0;
	private int lastCookTime1 = 0;
	private int lastCookTime2 = 0;
	private int lastEnergy = 0;

	public Retronism_ContainerMegaCrusher(InventoryPlayer playerInv, Retronism_TileMegaCrusher megaCrusher) {
		this.megaCrusher = megaCrusher;
		// Input slots
		this.addSlot(new Slot(megaCrusher, 0, 56, 17));
		this.addSlot(new Slot(megaCrusher, 2, 56, 39));
		this.addSlot(new Slot(megaCrusher, 4, 56, 61));
		// Output slots
		this.addSlot(new Retronism_SlotCrusher(playerInv.player, megaCrusher, 1, 116, 17));
		this.addSlot(new Retronism_SlotCrusher(playerInv.player, megaCrusher, 3, 116, 39));
		this.addSlot(new Retronism_SlotCrusher(playerInv.player, megaCrusher, 5, 116, 61));

		// Player inventory
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 9; ++j) {
				this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
			}
		}
		for (int i = 0; i < 9; ++i) {
			this.addSlot(new Slot(playerInv, i, 8 + i * 18, 142));
		}
	}

	public void updateCraftingResults() {
		super.updateCraftingResults();
		for (int i = 0; i < this.field_20121_g.size(); ++i) {
			ICrafting crafter = (ICrafting) this.field_20121_g.get(i);
			if (lastCookTime0 != megaCrusher.cookTime[0])
				crafter.func_20158_a(this, 0, megaCrusher.cookTime[0]);
			if (lastCookTime1 != megaCrusher.cookTime[1])
				crafter.func_20158_a(this, 1, megaCrusher.cookTime[1]);
			if (lastCookTime2 != megaCrusher.cookTime[2])
				crafter.func_20158_a(this, 2, megaCrusher.cookTime[2]);
			if (lastEnergy != megaCrusher.storedEnergy)
				crafter.func_20158_a(this, 3, megaCrusher.storedEnergy);
		}
		lastCookTime0 = megaCrusher.cookTime[0];
		lastCookTime1 = megaCrusher.cookTime[1];
		lastCookTime2 = megaCrusher.cookTime[2];
		lastEnergy = megaCrusher.storedEnergy;
	}

	public void func_20112_a(int id, int value) {
		if (id == 0) megaCrusher.cookTime[0] = value;
		if (id == 1) megaCrusher.cookTime[1] = value;
		if (id == 2) megaCrusher.cookTime[2] = value;
		if (id == 3) megaCrusher.storedEnergy = value;
	}

	public boolean isUsableByPlayer(EntityPlayer player) {
		return megaCrusher.canInteractWith(player);
	}

	public ItemStack getStackInSlot(int slotIndex) {
		ItemStack result = null;
		Slot slot = (Slot) this.slots.get(slotIndex);
		if (slot != null && slot.getHasStack()) {
			ItemStack slotStack = slot.getStack();
			result = slotStack.copy();
			// Slots 0-2 = inputs, 3-5 = outputs, 6-32 = player inv, 33-41 = hotbar
			if (slotIndex >= 3 && slotIndex <= 5) {
				// Output -> player
				this.func_28125_a(slotStack, 6, 42, true);
			} else if (slotIndex >= 6 && slotIndex < 33) {
				// Player inv -> hotbar
				this.func_28125_a(slotStack, 33, 42, false);
			} else if (slotIndex >= 33 && slotIndex < 42) {
				// Hotbar -> player inv
				this.func_28125_a(slotStack, 6, 33, false);
			} else {
				// Input -> player
				this.func_28125_a(slotStack, 6, 42, false);
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
