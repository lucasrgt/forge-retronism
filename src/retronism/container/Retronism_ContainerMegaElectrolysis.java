package retronism.container;

import net.minecraft.src.*;
import retronism.tile.Retronism_TileMegaElectrolysis;

public class Retronism_ContainerMegaElectrolysis extends Container {

	private Retronism_TileMegaElectrolysis tile;
	private int lastEnergy = -1;
	private int lastWater = -1;
	private int lastHydrogen = -1;
	private int lastOxygen = -1;
	private int lastHeavyWater = -1;
	private int lastProcessTime = -1;

	public Retronism_ContainerMegaElectrolysis(InventoryPlayer playerInv, Retronism_TileMegaElectrolysis tile) {
		this.tile = tile;

		// Player inventory (3 rows)
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}

		// Hotbar
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
		}
	}

	public boolean isUsableByPlayer(EntityPlayer player) {
		return tile.canInteractWith(player);
	}

	public void updateCraftingResults() {
		super.updateCraftingResults();
		for (int j = 0; j < this.field_20121_g.size(); j++) {
			ICrafting crafter = (ICrafting) this.field_20121_g.get(j);
			if (lastEnergy != tile.storedEnergy) crafter.func_20158_a(this, 0, tile.storedEnergy);
			if (lastWater != tile.waterStored) crafter.func_20158_a(this, 1, tile.waterStored);
			if (lastHydrogen != tile.hydrogenStored) crafter.func_20158_a(this, 2, tile.hydrogenStored);
			if (lastOxygen != tile.oxygenStored) crafter.func_20158_a(this, 3, tile.oxygenStored);
			if (lastHeavyWater != tile.heavyWaterStored) crafter.func_20158_a(this, 4, tile.heavyWaterStored);
			if (lastProcessTime != tile.processTime) crafter.func_20158_a(this, 5, tile.processTime);
		}
		lastEnergy = tile.storedEnergy;
		lastWater = tile.waterStored;
		lastHydrogen = tile.hydrogenStored;
		lastOxygen = tile.oxygenStored;
		lastHeavyWater = tile.heavyWaterStored;
		lastProcessTime = tile.processTime;
	}

	public void func_20112_a(int id, int value) {
		if (id == 0) tile.storedEnergy = value;
		if (id == 1) tile.waterStored = value;
		if (id == 2) tile.hydrogenStored = value;
		if (id == 3) tile.oxygenStored = value;
		if (id == 4) tile.heavyWaterStored = value;
		if (id == 5) tile.processTime = value;
	}

	public ItemStack getStackInSlot(int slotIndex) {
		ItemStack result = null;
		Slot slot = (Slot) this.slots.get(slotIndex);
		if (slot != null && slot.getHasStack()) {
			ItemStack slotStack = slot.getStack();
			result = slotStack.copy();
			// 0-26 = player inv, 27-35 = hotbar
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
