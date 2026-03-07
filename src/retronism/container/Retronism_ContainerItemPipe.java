package retronism.container;

import net.minecraft.src.*;
import retronism.tile.*;
import retronism.slot.*;

public class Retronism_ContainerItemPipe extends Container {
	private Retronism_TileItemPipe pipe;

	public Retronism_ContainerItemPipe(InventoryPlayer playerInv, Retronism_TileItemPipe pipe) {
		this.pipe = pipe;

		// 9 ghost filter slots (3x3 grid at texture position 7,17 → container slot at +1 = 8,18)
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				this.addSlot(new Retronism_SlotGhost(pipe, row * 3 + col, 8 + col * 18, 18 + row * 18));
			}
		}

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

	public ItemStack func_27280_a(int slotIndex, int button, boolean shiftClick, EntityPlayer player) {
		// Ghost slots: copy item type, don't move items
		if (slotIndex >= 0 && slotIndex < 9) {
			ItemStack held = player.inventory.getItemStack();
			if (button == 1 || held == null) {
				pipe.setFilterSlot(slotIndex, null);
			} else {
				pipe.setFilterSlot(slotIndex, new ItemStack(held.itemID, 1, held.getItemDamage()));
			}
			return null;
		}
		return super.func_27280_a(slotIndex, button, shiftClick, player);
	}

	public boolean isUsableByPlayer(EntityPlayer player) {
		return true;
	}

	public ItemStack getStackInSlot(int slotIndex) {
		// Shift-click: move to player inventory only (no ghost slots)
		ItemStack result = null;
		Slot slot = (Slot) this.slots.get(slotIndex);
		if (slot != null && slot.getHasStack()) {
			ItemStack slotStack = slot.getStack();
			result = slotStack.copy();
			if (slotIndex < 9) {
				return null;
			} else if (slotIndex >= 9 && slotIndex < 36) {
				this.func_28125_a(slotStack, 36, 45, false);
			} else if (slotIndex >= 36 && slotIndex < 45) {
				this.func_28125_a(slotStack, 9, 36, false);
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
