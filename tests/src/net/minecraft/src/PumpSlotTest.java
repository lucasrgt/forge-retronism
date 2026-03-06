package net.minecraft.src;

import org.junit.Test;
import static org.junit.Assert.*;

public class PumpSlotTest {

	@Test
	public void testRejectsNullStack() {
		RetroNism_TilePump pump = new RetroNism_TilePump();
		RetroNism_SlotPumpBucket slot = new RetroNism_SlotPumpBucket(pump, 0, 0, 0);
		assertFalse(slot.isItemValid(null));
	}

	@Test
	public void testSlotStackLimitIsOne() {
		RetroNism_TilePump pump = new RetroNism_TilePump();
		RetroNism_SlotPumpBucket slot = new RetroNism_SlotPumpBucket(pump, 0, 0, 0);
		assertEquals(1, slot.getSlotStackLimit());
	}
}
