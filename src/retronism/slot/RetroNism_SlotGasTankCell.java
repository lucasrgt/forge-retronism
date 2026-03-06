package retronism.slot;

import net.minecraft.src.*;
import retronism.*;

public class RetroNism_SlotGasTankCell extends Slot {

	public RetroNism_SlotGasTankCell(IInventory inventory, int slotIndex, int x, int y) {
		super(inventory, slotIndex, x, y);
	}

	public boolean isItemValid(ItemStack stack) {
		return stack != null && stack.itemID == mod_RetroNism.gasCellEmpty.shiftedIndex;
	}

	public int getSlotStackLimit() {
		return 1;
	}
}
