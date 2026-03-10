package net.minecraft.src;

import org.junit.Test;
import static org.junit.Assert.*;

public class GasTypeTest {

	@Test
	public void testConstants() {
		assertEquals(0, Aero_GasType.NONE);
		assertEquals(1, Aero_GasType.HYDROGEN);
		assertEquals(2, Aero_GasType.OXYGEN);
	}

	@Test
	public void testGetName() {
		assertEquals("None", Aero_GasType.getName(Aero_GasType.NONE));
		assertEquals("Hydrogen", Aero_GasType.getName(Aero_GasType.HYDROGEN));
		assertEquals("Oxygen", Aero_GasType.getName(Aero_GasType.OXYGEN));
	}

	@Test
	public void testGetColor() {
		assertNotEquals(0xFFFFFFFF, Aero_GasType.getColor(Aero_GasType.HYDROGEN));
		assertNotEquals(0xFFFFFFFF, Aero_GasType.getColor(Aero_GasType.OXYGEN));
		assertNotEquals(
			Aero_GasType.getColor(Aero_GasType.HYDROGEN),
			Aero_GasType.getColor(Aero_GasType.OXYGEN)
		);
	}
}
