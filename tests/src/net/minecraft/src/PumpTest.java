package net.minecraft.src;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class PumpTest {
	private RetroNism_TilePump pump;

	@Before
	public void setUp() {
		pump = new RetroNism_TilePump();
	}

	@Test
	public void testInitialState() {
		assertEquals(0, pump.getStoredEnergy());
		assertEquals(0, pump.getFluidAmount());
		assertEquals(RetroNism_FluidType.NONE, pump.getFluidType());
	}

	@Test
	public void testReceiveEnergy() {
		int accepted = pump.receiveEnergy(500);
		assertEquals(500, accepted);
		assertEquals(500, pump.getStoredEnergy());
	}

	@Test
	public void testEnergyCapacity() {
		int accepted = pump.receiveEnergy(999999);
		assertEquals(16000, accepted);
		assertEquals(16000, pump.getStoredEnergy());
	}

	@Test
	public void testRejectFluidInput() {
		int accepted = pump.receiveFluid(RetroNism_FluidType.WATER, 100);
		assertEquals("Pump should not accept fluid input (it produces fluid)", 0, accepted);
	}

	@Test
	public void testExtractWater() {
		// Manually set fluid via extractFluid behavior test
		// We can't easily set internal state, but we can test extract on empty
		int extracted = pump.extractFluid(RetroNism_FluidType.WATER, 100);
		assertEquals(0, extracted);
	}

	@Test
	public void testExtractWrongType() {
		int extracted = pump.extractFluid(999, 100);
		assertEquals(0, extracted);
	}

	@Test
	public void testFluidTypeWhenEmpty() {
		assertEquals(RetroNism_FluidType.NONE, pump.getFluidType());
	}

	@Test
	public void testFluidCapacity() {
		assertEquals(8000, pump.getFluidCapacity());
	}

	@Test
	public void testMaxEnergy() {
		assertEquals(16000, pump.getMaxEnergy());
	}

	@Test
	public void testOnlyPumpsSourceWater() {
		assertTrue(RetroNism_TilePump.isPumpableWaterBlock(9));  // still water
		assertFalse(RetroNism_TilePump.isPumpableWaterBlock(8)); // flowing water
		assertFalse(RetroNism_TilePump.isPumpableWaterBlock(0)); // air
	}
}
