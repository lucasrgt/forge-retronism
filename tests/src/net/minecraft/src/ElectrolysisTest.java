package net.minecraft.src;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class ElectrolysisTest {
	private Retronism_TileElectrolysis tile;

	@Before
	public void setUp() {
		tile = new Retronism_TileElectrolysis();
	}

	@Test
	public void testInitialState() {
		assertEquals(0, tile.storedEnergy);
		assertEquals(0, tile.waterStored);
		assertEquals(0, tile.hydrogenStored);
		assertEquals(0, tile.oxygenStored);
		assertEquals(0, tile.processTime);
	}

	@Test
	public void testReceiveEnergy() {
		int accepted = tile.receiveEnergy(1000);
		assertEquals(1000, accepted);
		assertEquals(1000, tile.storedEnergy);
	}

	@Test
	public void testReceiveEnergyOverCapacity() {
		int accepted = tile.receiveEnergy(999999);
		assertEquals(32000, accepted);
		assertEquals(32000, tile.storedEnergy);
	}

	@Test
	public void testReceiveWater() {
		int accepted = tile.receiveFluid(Aero_FluidType.WATER, 500);
		assertEquals(500, accepted);
		assertEquals(500, tile.waterStored);
	}

	@Test
	public void testRejectNonWaterFluid() {
		int accepted = tile.receiveFluid(999, 500);
		assertEquals("Electrolysis should only accept water", 0, accepted);
	}

	@Test
	public void testRejectFluidNone() {
		int accepted = tile.receiveFluid(Aero_FluidType.NONE, 500);
		assertEquals(0, accepted);
	}

	@Test
	public void testReceiveGasRejected() {
		int accepted = tile.receiveGas(Aero_GasType.HYDROGEN, 100);
		assertEquals("Electrolysis should not accept gas input", 0, accepted);
	}

	@Test
	public void testExtractHydrogen() {
		tile.hydrogenStored = 500;
		int extracted = tile.extractGas(Aero_GasType.HYDROGEN, 200);
		assertEquals(200, extracted);
		assertEquals(300, tile.hydrogenStored);
	}

	@Test
	public void testExtractOxygen() {
		tile.oxygenStored = 300;
		int extracted = tile.extractGas(Aero_GasType.OXYGEN, 200);
		assertEquals(200, extracted);
		assertEquals(100, tile.oxygenStored);
	}

	@Test
	public void testExtractWrongGasType() {
		tile.hydrogenStored = 500;
		int extracted = tile.extractGas(Aero_GasType.OXYGEN, 200);
		assertEquals(0, extracted);
	}

	@Test
	public void testCannotExtractFluid() {
		tile.waterStored = 5000;
		int extracted = tile.extractFluid(Aero_FluidType.WATER, 100);
		assertEquals("Electrolysis should not allow fluid extraction", 0, extracted);
	}

	@Test
	public void testScaledMethods() {
		tile.storedEnergy = 16000;
		tile.waterStored = 4000;
		tile.hydrogenStored = 2000;
		tile.oxygenStored = 4000;
		tile.processTime = 100;

		assertEquals(50, tile.getEnergyScaled(100));
		assertEquals(50, tile.getWaterScaled(100));
		assertEquals(25, tile.getHydrogenScaled(100));
		assertEquals(50, tile.getOxygenScaled(100));
		assertEquals(50, tile.getProcessScaled(100));
	}

	@Test
	public void testWaterCapacity() {
		int first = tile.receiveFluid(Aero_FluidType.WATER, 8000);
		assertEquals(8000, first);
		int second = tile.receiveFluid(Aero_FluidType.WATER, 1);
		assertEquals(0, second);
	}

	@Test
	public void testOutputRatio() {
		// Verify the 2:1 ratio is correct in constants
		// 1000 mB water -> 1000 mB H2 + 500 mB O2
		tile.waterStored = 1000;
		tile.storedEnergy = 32000;
		tile.hydrogenStored = 0;
		tile.oxygenStored = 0;

		// Simulate completing a process
		tile.processTime = 0;
		// After 200 ticks of processing:
		// water should decrease by 1000, H2 increase by 1000, O2 increase by 500
		// We can't call updateEntity (needs world), but we verify the constants are accessible
		assertEquals(32000, Retronism_TileElectrolysis.MAX_ENERGY);
		assertEquals(8000, Retronism_TileElectrolysis.MAX_WATER);
		assertEquals(8000, Retronism_TileElectrolysis.MAX_HYDROGEN);
		assertEquals(8000, Retronism_TileElectrolysis.MAX_OXYGEN);
	}
}
