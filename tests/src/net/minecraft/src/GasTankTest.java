package net.minecraft.src;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class GasTankTest {
	private RetroNism_TileGasTank tank;

	@Before
	public void setUp() {
		tank = new RetroNism_TileGasTank();
	}

	@Test
	public void testInitialState() {
		assertEquals(0, tank.getGasAmount());
		assertEquals(RetroNism_GasType.NONE, tank.getGasType());
		assertEquals(RetroNism_TileGasTank.MAX_GAS, tank.getGasCapacity());
	}

	@Test
	public void testReceiveGasAndType() {
		int accepted = tank.receiveGas(RetroNism_GasType.HYDROGEN, 900);
		assertEquals(900, accepted);
		assertEquals(900, tank.getGasAmount());
		assertEquals(RetroNism_GasType.HYDROGEN, tank.getGasType());
	}

	@Test
	public void testRejectDifferentGasType() {
		tank.receiveGas(RetroNism_GasType.HYDROGEN, 500);
		int accepted = tank.receiveGas(RetroNism_GasType.OXYGEN, 500);
		assertEquals(0, accepted);
		assertEquals(RetroNism_GasType.HYDROGEN, tank.getGasType());
	}

	@Test
	public void testExtractGas() {
		tank.receiveGas(RetroNism_GasType.OXYGEN, 1200);
		int extracted = tank.extractGas(RetroNism_GasType.OXYGEN, 1000);
		assertEquals(1000, extracted);
		assertEquals(200, tank.getGasAmount());
	}

	@Test
	public void testExtractClearsType() {
		tank.receiveGas(RetroNism_GasType.OXYGEN, 200);
		int extracted = tank.extractGas(RetroNism_GasType.OXYGEN, 200);
		assertEquals(200, extracted);
		assertEquals(0, tank.getGasAmount());
		assertEquals(RetroNism_GasType.NONE, tank.getGasType());
	}
}
