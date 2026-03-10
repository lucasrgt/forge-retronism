package net.minecraft.src;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class FluidPipeTest {
	private Retronism_TileFluidPipe pipe;

	@Before
	public void setUp() {
		pipe = new Retronism_TileFluidPipe();
	}

	@Test
	public void testInitialState() {
		assertEquals(Aero_FluidType.NONE, pipe.getFluidType());
		assertEquals(0, pipe.getFluidAmount());
		assertEquals(500, pipe.getFluidCapacity());
	}

	@Test
	public void testReceiveWater() {
		int accepted = pipe.receiveFluid(Aero_FluidType.WATER, 100);
		assertEquals(100, accepted);
		assertEquals(Aero_FluidType.WATER, pipe.getFluidType());
		assertEquals(100, pipe.getFluidAmount());
	}

	@Test
	public void testReceiveNone() {
		int accepted = pipe.receiveFluid(Aero_FluidType.NONE, 100);
		assertEquals(0, accepted);
		assertEquals(0, pipe.getFluidAmount());
	}

	@Test
	public void testReceiveOverCapacity() {
		int accepted = pipe.receiveFluid(Aero_FluidType.WATER, 9999);
		// Capped by both capacity (500) and transfer rate (200)
		assertEquals(200, accepted);
	}

	@Test
	public void testReceiveRespectTransferRate() {
		// First receive fills transfer rate
		int first = pipe.receiveFluid(Aero_FluidType.WATER, 200);
		assertEquals(200, first);
		// Second receive in same tick should be rejected (transfer rate exhausted)
		int second = pipe.receiveFluid(Aero_FluidType.WATER, 100);
		assertEquals(0, second);
	}

	@Test
	public void testExtractFluid() {
		pipe.receiveFluid(Aero_FluidType.WATER, 100);
		int extracted = pipe.extractFluid(Aero_FluidType.WATER, 50);
		assertEquals(50, extracted);
		assertEquals(50, pipe.getFluidAmount());
	}

	@Test
	public void testExtractWrongType() {
		pipe.receiveFluid(Aero_FluidType.WATER, 100);
		// Try extracting a type that doesn't match
		int extracted = pipe.extractFluid(999, 50);
		assertEquals(0, extracted);
		assertEquals(100, pipe.getFluidAmount());
	}

	@Test
	public void testExtractAllResetsType() {
		pipe.receiveFluid(Aero_FluidType.WATER, 100);
		pipe.extractFluid(Aero_FluidType.WATER, 100);
		assertEquals(0, pipe.getFluidAmount());
		assertEquals(Aero_FluidType.NONE, pipe.getFluidType());
	}

	@Test
	public void testExtractMoreThanAvailable() {
		pipe.receiveFluid(Aero_FluidType.WATER, 50);
		int extracted = pipe.extractFluid(Aero_FluidType.WATER, 200);
		assertEquals(50, extracted);
		assertEquals(0, pipe.getFluidAmount());
	}
}
