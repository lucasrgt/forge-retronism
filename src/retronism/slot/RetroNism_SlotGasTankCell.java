package retronism.slot;

import net.minecraft.src.*;
import retronism.*;

public class Retronism_SlotGasTankCell extends Slot {

	public Retronism_SlotGasTankCell(IInventory inventory, int slotIndex, int x, int y) {
		super(inventory, slotIndex, x, y);
	}

	public boolean isItemValid(ItemStack stack) {
		return stack != null && stack.itemID == mod_Retronism.gasCellEmpty.shiftedIndex;
	}

	public int getSlotStackLimit() {
		return 1;
	}
}
