package net.minecraft.src;

import org.junit.Test;
import static org.junit.Assert.*;

public class GasTypeTest {

	@Test
	public void testConstants() {
		assertEquals(0, RetroNism_GasType.NONE);
		assertEquals(1, RetroNism_GasType.HYDROGEN);
		assertEquals(2, RetroNism_GasType.OXYGEN);
	}

	@Test
	public void testGetName() {
		assertEquals("None", RetroNism_GasType.getName(RetroNism_GasType.NONE));
		assertEquals("Hydrogen", RetroNism_GasType.getName(RetroNism_GasType.HYDROGEN));
		assertEquals("Oxygen", RetroNism_GasType.getName(RetroNism_GasType.OXYGEN));
	}

	@Test
	public void testGetColor() {
		assertNotEquals(0xFFFFFFFF, RetroNism_GasType.getColor(RetroNism_GasType.HYDROGEN));
		assertNotEquals(0xFFFFFFFF, RetroNism_GasType.getColor(RetroNism_GasType.OXYGEN));
		assertNotEquals(
			RetroNism_GasType.getColor(RetroNism_GasType.HYDROGEN),
			RetroNism_GasType.getColor(RetroNism_GasType.OXYGEN)
		);
	}
}
