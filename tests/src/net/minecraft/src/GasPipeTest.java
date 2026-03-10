package net.minecraft.src;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class GasPipeTest {
	private Retronism_TileGasPipe pipe;

	@Before
	public void setUp() {
		pipe = new Retronism_TileGasPipe();
	}

	@Test
	public void testInitialState() {
		assertEquals(Aero_GasType.NONE, pipe.getGasType());
		assertEquals(0, pipe.getGasAmount());
		assertEquals(500, pipe.getGasCapacity());
	}

	@Test
	public void testReceiveHydrogen() {
		int accepted = pipe.receiveGas(Aero_GasType.HYDROGEN, 100);
		assertEquals(100, accepted);
		assertEquals(Aero_GasType.HYDROGEN, pipe.getGasType());
	}

	@Test
	public void testReceiveOxygen() {
		int accepted = pipe.receiveGas(Aero_GasType.OXYGEN, 100);
		assertEquals(100, accepted);
		assertEquals(Aero_GasType.OXYGEN, pipe.getGasType());
	}

	@Test
	public void testSingleTypeLock_RejectsDifferentGas() {
		pipe.receiveGas(Aero_GasType.HYDROGEN, 100);
		int accepted = pipe.receiveGas(Aero_GasType.OXYGEN, 100);
		assertEquals("Gas pipe must reject different gas type", 0, accepted);
		assertEquals(Aero_GasType.HYDROGEN, pipe.getGasType());
		assertEquals(100, pipe.getGasAmount());
	}

	@Test
	public void testSingleTypeLock_AcceptsSameGas() {
		pipe.receiveGas(Aero_GasType.HYDROGEN, 100);
		int accepted = pipe.receiveGas(Aero_GasType.HYDROGEN, 50);
		assertEquals(50, accepted);
		assertEquals(150, pipe.getGasAmount());
	}

	@Test
	public void testSingleTypeLock_UnlocksWhenEmpty() {
		pipe.receiveGas(Aero_GasType.HYDROGEN, 100);
		pipe.extractGas(Aero_GasType.HYDROGEN, 100);
		assertEquals(Aero_GasType.NONE, pipe.getGasType());

		// Now can accept oxygen
		int accepted = pipe.receiveGas(Aero_GasType.OXYGEN, 50);
		assertEquals(50, accepted);
		assertEquals(Aero_GasType.OXYGEN, pipe.getGasType());
	}

	@Test
	public void testReceiveNone() {
		int accepted = pipe.receiveGas(Aero_GasType.NONE, 100);
		assertEquals(0, accepted);
	}

	@Test
	public void testExtractWrongType() {
		pipe.receiveGas(Aero_GasType.HYDROGEN, 100);
		int extracted = pipe.extractGas(Aero_GasType.OXYGEN, 50);
		assertEquals(0, extracted);
	}

	@Test
	public void testTransferRateLimit() {
		int first = pipe.receiveGas(Aero_GasType.HYDROGEN, 200);
		assertEquals(200, first);
		int second = pipe.receiveGas(Aero_GasType.HYDROGEN, 100);
		assertEquals(0, second);
	}
}
