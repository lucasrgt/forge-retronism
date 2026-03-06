package net.minecraft.src;

import org.junit.Test;
import static org.junit.Assert.*;

public class FluidTypeTest {

	@Test
	public void testConstants() {
		assertEquals(0, RetroNism_FluidType.NONE);
		assertEquals(1, RetroNism_FluidType.WATER);
	}

	@Test
	public void testGetName() {
		assertEquals("None", RetroNism_FluidType.getName(RetroNism_FluidType.NONE));
		assertEquals("Water", RetroNism_FluidType.getName(RetroNism_FluidType.WATER));
		assertEquals("None", RetroNism_FluidType.getName(999));
	}

	@Test
	public void testGetColor() {
		assertEquals(0xFF3344FF, RetroNism_FluidType.getColor(RetroNism_FluidType.WATER));
		assertEquals(0xFFFFFFFF, RetroNism_FluidType.getColor(RetroNism_FluidType.NONE));
	}
}
