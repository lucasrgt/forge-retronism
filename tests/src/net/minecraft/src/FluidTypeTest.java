package net.minecraft.src;

import org.junit.Test;
import static org.junit.Assert.*;

public class FluidTypeTest {

	@Test
	public void testConstants() {
		assertEquals(0, Retronism_FluidType.NONE);
		assertEquals(1, Retronism_FluidType.WATER);
	}

	@Test
	public void testGetName() {
		assertEquals("None", Retronism_FluidType.getName(Retronism_FluidType.NONE));
		assertEquals("Water", Retronism_FluidType.getName(Retronism_FluidType.WATER));
		assertEquals("None", Retronism_FluidType.getName(999));
	}

	@Test
	public void testGetColor() {
		assertEquals(0xFF3344FF, Retronism_FluidType.getColor(Retronism_FluidType.WATER));
		assertEquals(0xFFFFFFFF, Retronism_FluidType.getColor(Retronism_FluidType.NONE));
	}
}
