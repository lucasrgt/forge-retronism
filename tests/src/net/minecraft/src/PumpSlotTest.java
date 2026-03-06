package net.minecraft.src;

import org.junit.Test;
import static org.junit.Assert.*;

public class PumpSlotTest {

	@Test
	public void testRejectsNullStack() {
		Retronism_TilePump pump = new Retronism_TilePump();
		Retronism_SlotPumpBucket slot = new Retronism_SlotPumpBucket(pump, 0, 0, 0);
		assertFalse(slot.isItemValid(null));
	}

	@Test
	public void testSlotStackLimitIsOne() {
		Retronism_TilePump pump = new Retronism_TilePump();
		Retronism_SlotPumpBucket slot = new Retronism_SlotPumpBucket(pump, 0, 0, 0);
		assertEquals(1, slot.getSlotStackLimit());
	}
}
