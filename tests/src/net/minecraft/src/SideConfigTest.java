package net.minecraft.src;

import org.junit.Test;
import static org.junit.Assert.*;

public class SideConfigTest {

	@Test
	public void testModes() {
		assertEquals(0, Retronism_SideConfig.MODE_NONE);
		assertEquals(1, Retronism_SideConfig.MODE_INPUT);
		assertEquals(2, Retronism_SideConfig.MODE_OUTPUT);
		assertEquals(3, Retronism_SideConfig.MODE_INPUT_OUTPUT);
	}

	@Test
	public void testCycleMode() {
		assertEquals(Retronism_SideConfig.MODE_INPUT, Retronism_SideConfig.cycleMode(Retronism_SideConfig.MODE_NONE));
		assertEquals(Retronism_SideConfig.MODE_OUTPUT, Retronism_SideConfig.cycleMode(Retronism_SideConfig.MODE_INPUT));
		assertEquals(Retronism_SideConfig.MODE_INPUT_OUTPUT, Retronism_SideConfig.cycleMode(Retronism_SideConfig.MODE_OUTPUT));
		assertEquals(Retronism_SideConfig.MODE_NONE, Retronism_SideConfig.cycleMode(Retronism_SideConfig.MODE_INPUT_OUTPUT));
	}

	@Test
	public void testOppositeSide() {
		assertEquals(1, Retronism_SideConfig.oppositeSide(0)); // Bottom <-> Top
		assertEquals(0, Retronism_SideConfig.oppositeSide(1));
		assertEquals(3, Retronism_SideConfig.oppositeSide(2)); // North <-> South
		assertEquals(2, Retronism_SideConfig.oppositeSide(3));
		assertEquals(5, Retronism_SideConfig.oppositeSide(4)); // West <-> East
		assertEquals(4, Retronism_SideConfig.oppositeSide(5));
	}

	@Test
	public void testCanInputOutput() {
		assertFalse(Retronism_SideConfig.canInput(Retronism_SideConfig.MODE_NONE));
		assertTrue(Retronism_SideConfig.canInput(Retronism_SideConfig.MODE_INPUT));
		assertFalse(Retronism_SideConfig.canInput(Retronism_SideConfig.MODE_OUTPUT));
		assertTrue(Retronism_SideConfig.canInput(Retronism_SideConfig.MODE_INPUT_OUTPUT));

		assertFalse(Retronism_SideConfig.canOutput(Retronism_SideConfig.MODE_NONE));
		assertFalse(Retronism_SideConfig.canOutput(Retronism_SideConfig.MODE_INPUT));
		assertTrue(Retronism_SideConfig.canOutput(Retronism_SideConfig.MODE_OUTPUT));
		assertTrue(Retronism_SideConfig.canOutput(Retronism_SideConfig.MODE_INPUT_OUTPUT));
	}

	@Test
	public void testGetSet() {
		int[] config = new int[24];
		Retronism_SideConfig.set(config, Retronism_SideConfig.SIDE_NORTH, Retronism_SideConfig.TYPE_ENERGY, Retronism_SideConfig.MODE_OUTPUT);
		assertEquals(Retronism_SideConfig.MODE_OUTPUT,
			Retronism_SideConfig.get(config, Retronism_SideConfig.SIDE_NORTH, Retronism_SideConfig.TYPE_ENERGY));
		// Other slots should remain 0
		assertEquals(Retronism_SideConfig.MODE_NONE,
			Retronism_SideConfig.get(config, Retronism_SideConfig.SIDE_NORTH, Retronism_SideConfig.TYPE_FLUID));
		assertEquals(Retronism_SideConfig.MODE_NONE,
			Retronism_SideConfig.get(config, Retronism_SideConfig.SIDE_SOUTH, Retronism_SideConfig.TYPE_ENERGY));
	}

	@Test
	public void testGetModeName() {
		assertEquals("Off", Retronism_SideConfig.getModeName(Retronism_SideConfig.MODE_NONE));
		assertEquals("Input", Retronism_SideConfig.getModeName(Retronism_SideConfig.MODE_INPUT));
		assertEquals("Output", Retronism_SideConfig.getModeName(Retronism_SideConfig.MODE_OUTPUT));
		assertEquals("I/O", Retronism_SideConfig.getModeName(Retronism_SideConfig.MODE_INPUT_OUTPUT));
	}

	@Test
	public void testGetColor() {
		assertEquals(Retronism_SideConfig.COLOR_NONE,
			Retronism_SideConfig.getColor(Retronism_SideConfig.TYPE_ENERGY, Retronism_SideConfig.MODE_NONE));
		assertEquals(Retronism_SideConfig.COLOR_ENERGY_IN,
			Retronism_SideConfig.getColor(Retronism_SideConfig.TYPE_ENERGY, Retronism_SideConfig.MODE_INPUT));
		assertEquals(Retronism_SideConfig.COLOR_ENERGY_OUT,
			Retronism_SideConfig.getColor(Retronism_SideConfig.TYPE_ENERGY, Retronism_SideConfig.MODE_OUTPUT));
		assertEquals(Retronism_SideConfig.COLOR_ENERGY_IO,
			Retronism_SideConfig.getColor(Retronism_SideConfig.TYPE_ENERGY, Retronism_SideConfig.MODE_INPUT_OUTPUT));
	}

	@Test
	public void testPumpDefaults() {
		Retronism_TilePump pump = new Retronism_TilePump();
		int[] config = pump.getSideConfig();
		for (int s = 0; s < 6; s++) {
			assertEquals("Pump energy should be INPUT on side " + s,
				Retronism_SideConfig.MODE_INPUT,
				Retronism_SideConfig.get(config, s, Retronism_SideConfig.TYPE_ENERGY));
			assertEquals("Pump fluid should be OUTPUT on side " + s,
				Retronism_SideConfig.MODE_OUTPUT,
				Retronism_SideConfig.get(config, s, Retronism_SideConfig.TYPE_FLUID));
			assertEquals("Pump item should be I/O on side " + s,
				Retronism_SideConfig.MODE_INPUT_OUTPUT,
				Retronism_SideConfig.get(config, s, Retronism_SideConfig.TYPE_ITEM));
		}
	}

	@Test
	public void testCableDefaults() {
		Retronism_TileCable cable = new Retronism_TileCable();
		int[] config = cable.getSideConfig();
		for (int s = 0; s < 6; s++) {
			assertEquals("Cable energy should be I/O on side " + s,
				Retronism_SideConfig.MODE_INPUT_OUTPUT,
				Retronism_SideConfig.get(config, s, Retronism_SideConfig.TYPE_ENERGY));
		}
	}

	@Test
	public void testSupportsType() {
		Retronism_TilePump pump = new Retronism_TilePump();
		assertTrue(pump.supportsType(Retronism_SideConfig.TYPE_ENERGY));
		assertTrue(pump.supportsType(Retronism_SideConfig.TYPE_FLUID));
		assertFalse(pump.supportsType(Retronism_SideConfig.TYPE_GAS));
		assertTrue(pump.supportsType(Retronism_SideConfig.TYPE_ITEM));
	}

	@Test
	public void testSetSideMode() {
		Retronism_TilePump pump = new Retronism_TilePump();
		pump.setSideMode(Retronism_SideConfig.SIDE_NORTH, Retronism_SideConfig.TYPE_ENERGY, Retronism_SideConfig.MODE_NONE);
		assertEquals(Retronism_SideConfig.MODE_NONE,
			Retronism_SideConfig.get(pump.getSideConfig(), Retronism_SideConfig.SIDE_NORTH, Retronism_SideConfig.TYPE_ENERGY));
		// Unsupported type should not change
		pump.setSideMode(Retronism_SideConfig.SIDE_NORTH, Retronism_SideConfig.TYPE_GAS, Retronism_SideConfig.MODE_INPUT);
		assertEquals(Retronism_SideConfig.MODE_NONE,
			Retronism_SideConfig.get(pump.getSideConfig(), Retronism_SideConfig.SIDE_NORTH, Retronism_SideConfig.TYPE_GAS));
	}
}
