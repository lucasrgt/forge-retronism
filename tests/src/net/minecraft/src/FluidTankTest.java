package net.minecraft.src;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class FluidTankTest {
	private Retronism_TileFluidTank tank;

	@Before
	public void setUp() {
		tank = new Retronism_TileFluidTank();
	}

	@Test
	public void testInitialState() {
		assertEquals(0, tank.getFluidAmount());
		assertEquals(Aero_FluidType.NONE, tank.getFluidType());
		assertEquals(Retronism_TileFluidTank.MAX_FLUID, tank.getFluidCapacity());
	}

	@Test
	public void testReceiveWater() {
		int accepted = tank.receiveFluid(Aero_FluidType.WATER, 1500);
		assertEquals(1500, accepted);
		assertEquals(1500, tank.getFluidAmount());
		assertEquals(Aero_FluidType.WATER, tank.getFluidType());
	}

	@Test
	public void testRejectInvalidFluid() {
		int accepted = tank.receiveFluid(Aero_FluidType.NONE, 500);
		assertEquals(0, accepted);
		assertEquals(0, tank.getFluidAmount());
	}

	@Test
	public void testCapacityClamp() {
		int accepted = tank.receiveFluid(Aero_FluidType.WATER, 99999);
		assertEquals(Retronism_TileFluidTank.MAX_FLUID, accepted);
		assertEquals(Retronism_TileFluidTank.MAX_FLUID, tank.getFluidAmount());
	}

	@Test
	public void testExtractFluid() {
		tank.receiveFluid(Aero_FluidType.WATER, 1200);
		int extracted = tank.extractFluid(Aero_FluidType.WATER, 1000);
		assertEquals(1000, extracted);
		assertEquals(200, tank.getFluidAmount());
	}
}
