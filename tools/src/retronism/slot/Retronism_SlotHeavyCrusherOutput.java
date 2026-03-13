package retronism.slot;

import net.minecraft.src.*;

public class Retronism_SlotHeavyCrusherOutput extends Slot {
    public Retronism_SlotHeavyCrusherOutput(IInventory inv, int slotIndex, int x, int y) {
        super(inv, slotIndex, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }
}
