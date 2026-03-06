package retronism.slot;

import net.minecraft.src.*;

public class RetroNism_SlotPumpBucket extends Slot {

	public RetroNism_SlotPumpBucket(IInventory inventory, int slotIndex, int x, int y) {
		super(inventory, slotIndex, x, y);
	}

	public boolean isItemValid(ItemStack stack) {
		return stack != null && stack.itemID == Item.bucketEmpty.shiftedIndex;
	}

	public int getSlotStackLimit() {
		return 1;
	}
}
