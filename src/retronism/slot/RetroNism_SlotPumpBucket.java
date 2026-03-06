package retronism.slot;

import net.minecraft.src.*;

public class Retronism_SlotPumpBucket extends Slot {

	public Retronism_SlotPumpBucket(IInventory inventory, int slotIndex, int x, int y) {
		super(inventory, slotIndex, x, y);
	}

	public boolean isItemValid(ItemStack stack) {
		return stack != null && stack.itemID == Item.bucketEmpty.shiftedIndex;
	}

	public int getSlotStackLimit() {
		return 1;
	}
}
