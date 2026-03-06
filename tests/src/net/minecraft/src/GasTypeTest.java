package net.minecraft.src;

import org.junit.Test;
import static org.junit.Assert.*;

public class GasTypeTest {

	@Test
	public void testConstants() {
		assertEquals(0, Retronism_GasType.NONE);
		assertEquals(1, Retronism_GasType.HYDROGEN);
		assertEquals(2, Retronism_GasType.OXYGEN);
	}

	@Test
	public void testGetName() {
		assertEquals("None", Retronism_GasType.getName(Retronism_GasType.NONE));
		assertEquals("Hydrogen", Retronism_GasType.getName(Retronism_GasType.HYDROGEN));
		assertEquals("Oxygen", Retronism_GasType.getName(Retronism_GasType.OXYGEN));
	}

	@Test
	public void testGetColor() {
		assertNotEquals(0xFFFFFFFF, Retronism_GasType.getColor(Retronism_GasType.HYDROGEN));
		assertNotEquals(0xFFFFFFFF, Retronism_GasType.getColor(Retronism_GasType.OXYGEN));
		assertNotEquals(
			Retronism_GasType.getColor(Retronism_GasType.HYDROGEN),
			Retronism_GasType.getColor(Retronism_GasType.OXYGEN)
		);
	}
}
