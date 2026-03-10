package net.minecraft.src;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class MegaPipeItemTest {
	private Retronism_TileMegaPipe pipe;

	private static final int DIRT_ID = 3;
	private static final int STONE_ID = 1;

	@Before
	public void setUp() {
		pipe = new Retronism_TileMegaPipe();
	}

	private ItemStack makeStack(int id, int count) {
		return new ItemStack(id, count, 0);
	}

	@Test
	public void testItemBufferStartsNull() {
		assertNull(pipe.itemBuffer);
	}

	@Test
	public void testReceiveItemSetsBuffer() {
		pipe.receiveItem(makeStack(DIRT_ID, 1), 2);
		assertNotNull(pipe.itemBuffer);
		assertEquals(DIRT_ID, pipe.itemBuffer.itemID);
		assertEquals(1, pipe.itemBuffer.stackSize);
	}

	@Test
	public void testReceiveItemFromDifferentSides() {
		pipe.receiveItem(makeStack(DIRT_ID, 1), 0);
		assertNotNull(pipe.itemBuffer);

		pipe.itemBuffer = null;
		pipe.receiveItem(makeStack(STONE_ID, 1), 4);
		assertNotNull(pipe.itemBuffer);
		assertEquals(STONE_ID, pipe.itemBuffer.itemID);
	}

	@Test
	public void testSupportsAllTypes() {
		assertTrue(pipe.supportsType(Aero_SideConfig.TYPE_ENERGY));
		assertTrue(pipe.supportsType(Aero_SideConfig.TYPE_FLUID));
		assertTrue(pipe.supportsType(Aero_SideConfig.TYPE_GAS));
		assertTrue(pipe.supportsType(Aero_SideConfig.TYPE_ITEM));
	}

	@Test
	public void testSideConfigDefaultsToIO() {
		int[] config = pipe.getSideConfig();
		for (int side = 0; side < 6; side++) {
			assertEquals(Aero_SideConfig.MODE_INPUT_OUTPUT,
				Aero_SideConfig.get(config, side, Aero_SideConfig.TYPE_ITEM));
			assertEquals(Aero_SideConfig.MODE_INPUT_OUTPUT,
				Aero_SideConfig.get(config, side, Aero_SideConfig.TYPE_ENERGY));
			assertEquals(Aero_SideConfig.MODE_INPUT_OUTPUT,
				Aero_SideConfig.get(config, side, Aero_SideConfig.TYPE_FLUID));
			assertEquals(Aero_SideConfig.MODE_INPUT_OUTPUT,
				Aero_SideConfig.get(config, side, Aero_SideConfig.TYPE_GAS));
		}
	}

	@Test
	public void testEnergyReceive() {
		int accepted = pipe.receiveEnergy(100);
		assertEquals(100, accepted);
		assertEquals(100, pipe.getStoredEnergy());
	}

	@Test
	public void testEnergyReceiveOverCapacity() {
		int accepted = pipe.receiveEnergy(9999);
		assertEquals(200, accepted);
		assertEquals(200, pipe.getStoredEnergy());
	}

	@Test
	public void testFluidReceive() {
		int accepted = pipe.receiveFluid(Aero_FluidType.WATER, 100);
		assertEquals(100, accepted);
		assertEquals(100, pipe.getFluidAmount());
		assertEquals(Aero_FluidType.WATER, pipe.getFluidType());
	}

	@Test
	public void testGasReceive() {
		int accepted = pipe.receiveGas(Aero_GasType.HYDROGEN, 100);
		assertEquals(100, accepted);
		assertEquals(100, pipe.getGasAmount());
		assertEquals(Aero_GasType.HYDROGEN, pipe.getGasType());
	}
}
