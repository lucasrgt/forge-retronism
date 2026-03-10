package net.minecraft.src;

import org.junit.Test;
import static org.junit.Assert.*;

public class SideConfigTest {

	@Test
	public void testModes() {
		assertEquals(0, Aero_SideConfig.MODE_NONE);
		assertEquals(1, Aero_SideConfig.MODE_INPUT);
		assertEquals(2, Aero_SideConfig.MODE_OUTPUT);
		assertEquals(3, Aero_SideConfig.MODE_INPUT_OUTPUT);
	}

	@Test
	public void testCycleMode() {
		assertEquals(Aero_SideConfig.MODE_INPUT, Aero_SideConfig.cycleMode(Aero_SideConfig.MODE_NONE));
		assertEquals(Aero_SideConfig.MODE_OUTPUT, Aero_SideConfig.cycleMode(Aero_SideConfig.MODE_INPUT));
		assertEquals(Aero_SideConfig.MODE_INPUT_OUTPUT, Aero_SideConfig.cycleMode(Aero_SideConfig.MODE_OUTPUT));
		assertEquals(Aero_SideConfig.MODE_NONE, Aero_SideConfig.cycleMode(Aero_SideConfig.MODE_INPUT_OUTPUT));
	}

	@Test
	public void testOppositeSide() {
		assertEquals(1, Aero_SideConfig.oppositeSide(0)); // Bottom <-> Top
		assertEquals(0, Aero_SideConfig.oppositeSide(1));
		assertEquals(3, Aero_SideConfig.oppositeSide(2)); // North <-> South
		assertEquals(2, Aero_SideConfig.oppositeSide(3));
		assertEquals(5, Aero_SideConfig.oppositeSide(4)); // West <-> East
		assertEquals(4, Aero_SideConfig.oppositeSide(5));
	}

	@Test
	public void testCanInputOutput() {
		assertFalse(Aero_SideConfig.canInput(Aero_SideConfig.MODE_NONE));
		assertTrue(Aero_SideConfig.canInput(Aero_SideConfig.MODE_INPUT));
		assertFalse(Aero_SideConfig.canInput(Aero_SideConfig.MODE_OUTPUT));
		assertTrue(Aero_SideConfig.canInput(Aero_SideConfig.MODE_INPUT_OUTPUT));

		assertFalse(Aero_SideConfig.canOutput(Aero_SideConfig.MODE_NONE));
		assertFalse(Aero_SideConfig.canOutput(Aero_SideConfig.MODE_INPUT));
		assertTrue(Aero_SideConfig.canOutput(Aero_SideConfig.MODE_OUTPUT));
		assertTrue(Aero_SideConfig.canOutput(Aero_SideConfig.MODE_INPUT_OUTPUT));
	}

	@Test
	public void testGetSet() {
		int[] config = new int[24];
		Aero_SideConfig.set(config, Aero_SideConfig.SIDE_NORTH, Aero_SideConfig.TYPE_ENERGY, Aero_SideConfig.MODE_OUTPUT);
		assertEquals(Aero_SideConfig.MODE_OUTPUT,
			Aero_SideConfig.get(config, Aero_SideConfig.SIDE_NORTH, Aero_SideConfig.TYPE_ENERGY));
		// Other slots should remain 0
		assertEquals(Aero_SideConfig.MODE_NONE,
			Aero_SideConfig.get(config, Aero_SideConfig.SIDE_NORTH, Aero_SideConfig.TYPE_FLUID));
		assertEquals(Aero_SideConfig.MODE_NONE,
			Aero_SideConfig.get(config, Aero_SideConfig.SIDE_SOUTH, Aero_SideConfig.TYPE_ENERGY));
	}

	@Test
	public void testGetModeName() {
		assertEquals("Off", Aero_SideConfig.getModeName(Aero_SideConfig.MODE_NONE));
		assertEquals("Input", Aero_SideConfig.getModeName(Aero_SideConfig.MODE_INPUT));
		assertEquals("Output", Aero_SideConfig.getModeName(Aero_SideConfig.MODE_OUTPUT));
		assertEquals("I/O", Aero_SideConfig.getModeName(Aero_SideConfig.MODE_INPUT_OUTPUT));
	}

	@Test
	public void testGetColor() {
		assertEquals(Aero_SideConfig.COLOR_NONE,
			Aero_SideConfig.getColor(Aero_SideConfig.TYPE_ENERGY, Aero_SideConfig.MODE_NONE));
		assertEquals(Aero_SideConfig.COLOR_ENERGY_IN,
			Aero_SideConfig.getColor(Aero_SideConfig.TYPE_ENERGY, Aero_SideConfig.MODE_INPUT));
		assertEquals(Aero_SideConfig.COLOR_ENERGY_OUT,
			Aero_SideConfig.getColor(Aero_SideConfig.TYPE_ENERGY, Aero_SideConfig.MODE_OUTPUT));
		assertEquals(Aero_SideConfig.COLOR_ENERGY_IO,
			Aero_SideConfig.getColor(Aero_SideConfig.TYPE_ENERGY, Aero_SideConfig.MODE_INPUT_OUTPUT));
	}

	@Test
	public void testPumpDefaults() {
		Retronism_TilePump pump = new Retronism_TilePump();
		int[] config = pump.getSideConfig();
		for (int s = 0; s < 6; s++) {
			assertEquals("Pump energy should be INPUT on side " + s,
				Aero_SideConfig.MODE_INPUT,
				Aero_SideConfig.get(config, s, Aero_SideConfig.TYPE_ENERGY));
			assertEquals("Pump fluid should be OUTPUT on side " + s,
				Aero_SideConfig.MODE_OUTPUT,
				Aero_SideConfig.get(config, s, Aero_SideConfig.TYPE_FLUID));
			assertEquals("Pump item should be I/O on side " + s,
				Aero_SideConfig.MODE_INPUT_OUTPUT,
				Aero_SideConfig.get(config, s, Aero_SideConfig.TYPE_ITEM));
		}
	}

	@Test
	public void testCableDefaults() {
		Retronism_TileCable cable = new Retronism_TileCable();
		int[] config = cable.getSideConfig();
		for (int s = 0; s < 6; s++) {
			assertEquals("Cable energy should be I/O on side " + s,
				Aero_SideConfig.MODE_INPUT_OUTPUT,
				Aero_SideConfig.get(config, s, Aero_SideConfig.TYPE_ENERGY));
		}
	}

	@Test
	public void testSupportsType() {
		Retronism_TilePump pump = new Retronism_TilePump();
		assertTrue(pump.supportsType(Aero_SideConfig.TYPE_ENERGY));
		assertTrue(pump.supportsType(Aero_SideConfig.TYPE_FLUID));
		assertFalse(pump.supportsType(Aero_SideConfig.TYPE_GAS));
		assertTrue(pump.supportsType(Aero_SideConfig.TYPE_ITEM));
	}

	@Test
	public void testSetSideMode() {
		Retronism_TilePump pump = new Retronism_TilePump();
		pump.setSideMode(Aero_SideConfig.SIDE_NORTH, Aero_SideConfig.TYPE_ENERGY, Aero_SideConfig.MODE_NONE);
		assertEquals(Aero_SideConfig.MODE_NONE,
			Aero_SideConfig.get(pump.getSideConfig(), Aero_SideConfig.SIDE_NORTH, Aero_SideConfig.TYPE_ENERGY));
		// Unsupported type should not change
		pump.setSideMode(Aero_SideConfig.SIDE_NORTH, Aero_SideConfig.TYPE_GAS, Aero_SideConfig.MODE_INPUT);
		assertEquals(Aero_SideConfig.MODE_NONE,
			Aero_SideConfig.get(pump.getSideConfig(), Aero_SideConfig.SIDE_NORTH, Aero_SideConfig.TYPE_GAS));
	}
}
