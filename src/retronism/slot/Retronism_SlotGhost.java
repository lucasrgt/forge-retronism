package retronism.slot;

import net.minecraft.src.*;
import retronism.tile.*;

public class Retronism_SlotGhost extends Slot {
	private Retronism_TileItemPipe pipe;
	private int filterIndex;

	public Retronism_SlotGhost(Retronism_TileItemPipe pipe, int filterIndex, int x, int y) {
		super(pipe, 0, x, y);
		this.pipe = pipe;
		this.filterIndex = filterIndex;
	}

	public ItemStack getStack() {
		return pipe.getFilterSlot(filterIndex);
	}

	public void putStack(ItemStack stack) {
		if (stack != null) {
			pipe.setFilterSlot(filterIndex, new ItemStack(stack.itemID, 1, stack.getItemDamage()));
		} else {
			pipe.setFilterSlot(filterIndex, null);
		}
	}

	public ItemStack decrStackSize(int amount) {
		pipe.setFilterSlot(filterIndex, null);
		return null;
	}

	public boolean isItemValid(ItemStack stack) {
		return false;
	}

	public int getSlotStackLimit() {
		return 1;
	}

	public boolean getHasStack() {
		return pipe.getFilterSlot(filterIndex) != null;
	}

	public void onSlotChanged() {}

	public void onPickupFromSlot(ItemStack stack) {}
}
