package retronism.slot;

import net.minecraft.src.*;

public class Retronism_SlotFluidTankBucket extends Slot {

	public Retronism_SlotFluidTankBucket(IInventory inventory, int slotIndex, int x, int y) {
		super(inventory, slotIndex, x, y);
	}

	public boolean isItemValid(ItemStack stack) {
		return stack != null && stack.itemID == Item.bucketEmpty.shiftedIndex;
	}

	public int getSlotStackLimit() {
		return 1;
	}
}
