package net.minecraft.src;

import org.junit.Test;
import static org.junit.Assert.*;

public class SideConfigTest {

	@Test
	public void testModes() {
		assertEquals(0, RetroNism_SideConfig.MODE_NONE);
		assertEquals(1, RetroNism_SideConfig.MODE_INPUT);
		assertEquals(2, RetroNism_SideConfig.MODE_OUTPUT);
		assertEquals(3, RetroNism_SideConfig.MODE_INPUT_OUTPUT);
	}

	@Test
	public void testCycleMode() {
		assertEquals(RetroNism_SideConfig.MODE_INPUT, RetroNism_SideConfig.cycleMode(RetroNism_SideConfig.MODE_NONE));
		assertEquals(RetroNism_SideConfig.MODE_OUTPUT, RetroNism_SideConfig.cycleMode(RetroNism_SideConfig.MODE_INPUT));
		assertEquals(RetroNism_SideConfig.MODE_INPUT_OUTPUT, RetroNism_SideConfig.cycleMode(RetroNism_SideConfig.MODE_OUTPUT));
		assertEquals(RetroNism_SideConfig.MODE_NONE, RetroNism_SideConfig.cycleMode(RetroNism_SideConfig.MODE_INPUT_OUTPUT));
	}

	@Test
	public void testOppositeSide() {
		assertEquals(1, RetroNism_SideConfig.oppositeSide(0)); // Bottom <-> Top
		assertEquals(0, RetroNism_SideConfig.oppositeSide(1));
		assertEquals(3, RetroNism_SideConfig.oppositeSide(2)); // North <-> South
		assertEquals(2, RetroNism_SideConfig.oppositeSide(3));
		assertEquals(5, RetroNism_SideConfig.oppositeSide(4)); // West <-> East
		assertEquals(4, RetroNism_SideConfig.oppositeSide(5));
	}

	@Test
	public void testCanInputOutput() {
		assertFalse(RetroNism_SideConfig.canInput(RetroNism_SideConfig.MODE_NONE));
		assertTrue(RetroNism_SideConfig.canInput(RetroNism_SideConfig.MODE_INPUT));
		assertFalse(RetroNism_SideConfig.canInput(RetroNism_SideConfig.MODE_OUTPUT));
		assertTrue(RetroNism_SideConfig.canInput(RetroNism_SideConfig.MODE_INPUT_OUTPUT));

		assertFalse(RetroNism_SideConfig.canOutput(RetroNism_SideConfig.MODE_NONE));
		assertFalse(RetroNism_SideConfig.canOutput(RetroNism_SideConfig.MODE_INPUT));
		assertTrue(RetroNism_SideConfig.canOutput(RetroNism_SideConfig.MODE_OUTPUT));
		assertTrue(RetroNism_SideConfig.canOutput(RetroNism_SideConfig.MODE_INPUT_OUTPUT));
	}

	@Test
	public void testGetSet() {
		int[] config = new int[24];
		RetroNism_SideConfig.set(config, RetroNism_SideConfig.SIDE_NORTH, RetroNism_SideConfig.TYPE_ENERGY, RetroNism_SideConfig.MODE_OUTPUT);
		assertEquals(RetroNism_SideConfig.MODE_OUTPUT,
			RetroNism_SideConfig.get(config, RetroNism_SideConfig.SIDE_NORTH, RetroNism_SideConfig.TYPE_ENERGY));
		// Other slots should remain 0
		assertEquals(RetroNism_SideConfig.MODE_NONE,
			RetroNism_SideConfig.get(config, RetroNism_SideConfig.SIDE_NORTH, RetroNism_SideConfig.TYPE_FLUID));
		assertEquals(RetroNism_SideConfig.MODE_NONE,
			RetroNism_SideConfig.get(config, RetroNism_SideConfig.SIDE_SOUTH, RetroNism_SideConfig.TYPE_ENERGY));
	}

	@Test
	public void testGetModeName() {
		assertEquals("Off", RetroNism_SideConfig.getModeName(RetroNism_SideConfig.MODE_NONE));
		assertEquals("Input", RetroNism_SideConfig.getModeName(RetroNism_SideConfig.MODE_INPUT));
		assertEquals("Output", RetroNism_SideConfig.getModeName(RetroNism_SideConfig.MODE_OUTPUT));
		assertEquals("I/O", RetroNism_SideConfig.getModeName(RetroNism_SideConfig.MODE_INPUT_OUTPUT));
	}

	@Test
	public void testGetColor() {
		assertEquals(RetroNism_SideConfig.COLOR_NONE,
			RetroNism_SideConfig.getColor(RetroNism_SideConfig.TYPE_ENERGY, RetroNism_SideConfig.MODE_NONE));
		assertEquals(RetroNism_SideConfig.COLOR_ENERGY_IN,
			RetroNism_SideConfig.getColor(RetroNism_SideConfig.TYPE_ENERGY, RetroNism_SideConfig.MODE_INPUT));
		assertEquals(RetroNism_SideConfig.COLOR_ENERGY_OUT,
			RetroNism_SideConfig.getColor(RetroNism_SideConfig.TYPE_ENERGY, RetroNism_SideConfig.MODE_OUTPUT));
		assertEquals(RetroNism_SideConfig.COLOR_ENERGY_IO,
			RetroNism_SideConfig.getColor(RetroNism_SideConfig.TYPE_ENERGY, RetroNism_SideConfig.MODE_INPUT_OUTPUT));
	}

	@Test
	public void testPumpDefaults() {
		RetroNism_TilePump pump = new RetroNism_TilePump();
		int[] config = pump.getSideConfig();
		for (int s = 0; s < 6; s++) {
			assertEquals("Pump energy should be INPUT on side " + s,
				RetroNism_SideConfig.MODE_INPUT,
				RetroNism_SideConfig.get(config, s, RetroNism_SideConfig.TYPE_ENERGY));
			assertEquals("Pump fluid should be OUTPUT on side " + s,
				RetroNism_SideConfig.MODE_OUTPUT,
				RetroNism_SideConfig.get(config, s, RetroNism_SideConfig.TYPE_FLUID));
			assertEquals("Pump item should be I/O on side " + s,
				RetroNism_SideConfig.MODE_INPUT_OUTPUT,
				RetroNism_SideConfig.get(config, s, RetroNism_SideConfig.TYPE_ITEM));
		}
	}

	@Test
	public void testCableDefaults() {
		RetroNism_TileCable cable = new RetroNism_TileCable();
		int[] config = cable.getSideConfig();
		for (int s = 0; s < 6; s++) {
			assertEquals("Cable energy should be I/O on side " + s,
				RetroNism_SideConfig.MODE_INPUT_OUTPUT,
				RetroNism_SideConfig.get(config, s, RetroNism_SideConfig.TYPE_ENERGY));
		}
	}

	@Test
	public void testSupportsType() {
		RetroNism_TilePump pump = new RetroNism_TilePump();
		assertTrue(pump.supportsType(RetroNism_SideConfig.TYPE_ENERGY));
		assertTrue(pump.supportsType(RetroNism_SideConfig.TYPE_FLUID));
		assertFalse(pump.supportsType(RetroNism_SideConfig.TYPE_GAS));
		assertTrue(pump.supportsType(RetroNism_SideConfig.TYPE_ITEM));
	}

	@Test
	public void testSetSideMode() {
		RetroNism_TilePump pump = new RetroNism_TilePump();
		pump.setSideMode(RetroNism_SideConfig.SIDE_NORTH, RetroNism_SideConfig.TYPE_ENERGY, RetroNism_SideConfig.MODE_NONE);
		assertEquals(RetroNism_SideConfig.MODE_NONE,
			RetroNism_SideConfig.get(pump.getSideConfig(), RetroNism_SideConfig.SIDE_NORTH, RetroNism_SideConfig.TYPE_ENERGY));
		// Unsupported type should not change
		pump.setSideMode(RetroNism_SideConfig.SIDE_NORTH, RetroNism_SideConfig.TYPE_GAS, RetroNism_SideConfig.MODE_INPUT);
		assertEquals(RetroNism_SideConfig.MODE_NONE,
			RetroNism_SideConfig.get(pump.getSideConfig(), RetroNism_SideConfig.SIDE_NORTH, RetroNism_SideConfig.TYPE_GAS));
	}
}
