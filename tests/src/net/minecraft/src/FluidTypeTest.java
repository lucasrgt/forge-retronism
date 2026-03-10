package net.minecraft.src;

import org.junit.Test;
import static org.junit.Assert.*;

public class FluidTypeTest {

	@Test
	public void testConstants() {
		assertEquals(0, Aero_FluidType.NONE);
		assertEquals(1, Aero_FluidType.WATER);
	}

	@Test
	public void testGetName() {
		assertEquals("None", Aero_FluidType.getName(Aero_FluidType.NONE));
		assertEquals("Water", Aero_FluidType.getName(Aero_FluidType.WATER));
		assertEquals("None", Aero_FluidType.getName(999));
	}

	@Test
	public void testGetColor() {
		assertEquals(0xFF3344FF, Aero_FluidType.getColor(Aero_FluidType.WATER));
		assertEquals(0xFFFFFFFF, Aero_FluidType.getColor(Aero_FluidType.NONE));
	}
}
