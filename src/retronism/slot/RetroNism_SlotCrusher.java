package retronism.slot;

import net.minecraft.src.*;

public class RetroNism_SlotCrusher extends Slot {
	private EntityPlayer thePlayer;

	public RetroNism_SlotCrusher(EntityPlayer player, IInventory inventory, int slotIndex, int x, int y) {
		super(inventory, slotIndex, x, y);
		this.thePlayer = player;
	}

	public boolean isItemValid(ItemStack stack) {
		return false;
	}

	public void onPickupFromSlot(ItemStack stack) {
		super.onPickupFromSlot(stack);
	}
}
